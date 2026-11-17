// Independent Unity IL2CPP diagnostics and a narrowly scoped world-time control.
// Does not import any code or routines from zsmvn.dylib.
// NOTE: World timeScale is NOT vehicle speed; all car/replay/match game hooks
// still require explicit, verified implementations for the target game version.
#include "ERIGameRuntime.h"
#include <dlfcn.h>
#include <mach-o/dyld.h>
#include <cstring>
#include <algorithm>
#include <string>
#include <cmath>
#include <cstdint>

namespace {
using IL2CPPDomain = void*;
using IL2CPPAssembly = void*;
using IL2CPPImage = void*;
using IL2CPPClass = void*;
using IL2CPPResolve = void* (*)(const char*);
using GetDomain = IL2CPPDomain (*)();
using GetAssemblies = const IL2CPPAssembly** (*)(IL2CPPDomain, size_t*);
using GetImage = IL2CPPImage (*)(const IL2CPPAssembly*);
using GetClassCount = size_t (*)(IL2CPPImage);
using GetClass = IL2CPPClass (*)(IL2CPPImage,size_t);
using GetClassName = const char* (*)(IL2CPPClass);
using GetTimeScale = float (*)();
using SetTimeScale = void (*)(float);

ERIGameRuntimeInfo gInfo;
GetTimeScale gGetTime = nullptr;
SetTimeScale gSetTime = nullptr;
float gOriginalTime = 1.0f;
bool gSavedOriginal = false;
bool gTimeActive = false;

void* unityHandle() {
    static void* handle = nullptr;
    if(handle) return handle;
    for(uint32_t i=0;i<_dyld_image_count();i++) {
        const char* path=_dyld_get_image_name(i);
        if(!path) continue;
        if(std::strstr(path,"UnityFramework") || std::strstr(path,"GameAssembly")) {
            void* loaded=dlopen(path,RTLD_LAZY | RTLD_NOLOAD);
            if(loaded) { handle=loaded; return handle; }
        }
    }
    return nullptr;
}
void* symbol(const char* name) {
    void* value=dlsym(RTLD_DEFAULT,name);
    if(value) return value;
    void* handle=unityHandle();
    return handle?dlsym(handle,name):nullptr;
}
void resolveTimeScale() {
    if(gSetTime && gGetTime) return;
    auto resolve=(IL2CPPResolve)symbol("il2cpp_resolve_icall");
    if(!resolve) return;
    // UnityEngine.Time has native internal calls on supported Unity versions.
    gGetTime=(GetTimeScale)resolve("UnityEngine.Time::get_timeScale()");
    gSetTime=(SetTimeScale)resolve("UnityEngine.Time::set_timeScale(System.Single)");
}
bool equals(const char* a,const char* b) {
    return a && b && std::strcmp(a,b)==0;
}
}

void ERIGameProbe(bool scanClasses) {
    resolveTimeScale();
    gInfo.il2cppLoaded=symbol("il2cpp_domain_get")!=nullptr;
    gInfo.timeScaleReady=gSetTime && gGetTime;
    gInfo.detail=gInfo.il2cppLoaded
        ? (gInfo.timeScaleReady?"IL2CPP and Unity Time icalls resolved."
                              :"IL2CPP found; Unity Time icalls unavailable.")
        : "IL2CPP exports not loaded or unavailable in this app.";
    if(!scanClasses || !gInfo.il2cppLoaded) return;
    // Read-only method/class metadata lookup. Never alter unknown game objects.
    auto domain=(GetDomain)symbol("il2cpp_domain_get");
    auto assemblies=(GetAssemblies)symbol("il2cpp_domain_get_assemblies");
    auto image=(GetImage)symbol("il2cpp_assembly_get_image");
    auto count=(GetClassCount)symbol("il2cpp_image_get_class_count");
    auto klass=(GetClass)symbol("il2cpp_image_get_class");
    auto name=(GetClassName)symbol("il2cpp_class_get_name");
    if(!domain || !assemblies || !image || !count || !klass || !name) {
        gInfo.detail="IL2CPP metadata scan API unavailable; no unsafe memory scan attempted.";
        return;
    }
    IL2CPPDomain d=domain();
    if(!d) {gInfo.detail="IL2CPP domain is not initialized.";return;}
    size_t countAssemblies=0;
    const IL2CPPAssembly** list=assemblies(d,&countAssemblies);
    if(!list || countAssemblies>1024) {
        gInfo.detail="Invalid or unavailable assembly list.";
        return;
    }
    gInfo.assemblies=(int)countAssemblies;
    gInfo.classesScanned=0;
    gInfo.vehicleMgrFound=false;
    gInfo.garageMgrFound=false;
    gInfo.matchMgrFound=false;
    gInfo.replayMgrFound=false;
    for(size_t ai=0;ai<countAssemblies && gInfo.classesScanned<180000;ai++) {
        IL2CPPImage img=image(list[ai]);
        if(!img) continue;
        size_t num=count(img);
        if(num>180000) continue;
        for(size_t ci=0;ci<num && gInfo.classesScanned<180000;ci++) {
            IL2CPPClass cls=klass(img,ci);
            if(!cls) continue;
            const char* n=name(cls);
            if(equals(n,"VehicleMgr")) gInfo.vehicleMgrFound=true;
            if(equals(n,"GarageMgr")) gInfo.garageMgrFound=true;
            if(equals(n,"MatchMgr") || equals(n,"QuickMatchMdl")) gInfo.matchMgrFound=true;
            if(equals(n,"MatchVideoMdl")) gInfo.replayMgrFound=true;
            gInfo.classesScanned++;
        }
    }
    gInfo.scannedClasses=true;
    gInfo.detail="Read-only class scan completed. A found class does NOT imply an implemented game hook.";
}
const ERIGameRuntimeInfo& ERIGameInfo() { return gInfo; }

bool ERIWorldTimeSet(float multiplier) {
    if(!std::isfinite(multiplier)||multiplier<0.5f||multiplier>2.0f) return false;
    resolveTimeScale();
    if(!gGetTime || !gSetTime) return false;
    if(!gSavedOriginal) {
        float current=gGetTime();
        if(!std::isfinite(current)||current<0.01f||current>10.f) return false;
        gOriginalTime=current;
        gSavedOriginal=true;
    }
    gSetTime(multiplier);
    gTimeActive=true;
    return true;
}
void ERIWorldTimeRestore() {
    if(gSavedOriginal && gSetTime) gSetTime(gOriginalTime);
    gSavedOriginal=false;
    gTimeActive=false;
}
bool ERIWorldTimeActive() { return gTimeActive; }
