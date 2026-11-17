#include "ERIPortDiagnostics.h"
#include <dlfcn.h>
#include <mach-o/dyld.h>
#include <cstring>
#include <string>
#include <sstream>
#include <algorithm>
#include <cstdint>

namespace {
using GDomain = void* (*)();
using GAssemblies = const void** (*)(void*, size_t*);
using GImage = void* (*)(const void*);
using GClassCount = size_t (*)(void*);
using GClass = void* (*)(void*, size_t);
using GName = const char* (*)(void*);
using GClassMethods = const void* (*)(void*, void**);
using GMethodName = const char* (*)(const void*);
using GParamCount = uint32_t (*)(const void*);
using GFields = const void* (*)(void*, void**);
using GFieldName = const char* (*)(const void*);
using GFieldOffset = int (*)(const void*);
using ThreadCurrent = void* (*)();
using ThreadAttach = void* (*)(void*);
using ThreadDetach = void (*)(void*);
void* lookup(const char* name) {
    void* ptr=dlsym(RTLD_DEFAULT,name);
    if(ptr) return ptr;
    for(uint32_t i=0;i<_dyld_image_count();i++) {
        const char* p=_dyld_get_image_name(i);
        if(!p || (!strstr(p,"UnityFramework") && !strstr(p,"GameAssembly")))continue;
        void* handle=dlopen(p,RTLD_LAZY|RTLD_NOLOAD);
        if(handle) {
            ptr=dlsym(handle,name);
            if(ptr)return ptr;
        }
    }
    return nullptr;
}
std::string clean(const char* str) {
    if(!str)return "<null>";
    std::string out;
    for(size_t i=0;str[i] && i<100;i++) {
        const unsigned char c=str[i];
        out.push_back((c<32||c==127)?'_':c);
    }
    return out;
}
bool interesting(const char* name) {
    if(!name)return false;
    static const char* want[]={
        "VehicleMgr","GarageMgr","MatchMgr","QuickMatchMdl","MatchVideoMdl",
        "Vehicle","Garage","Replay","MatchRoom","PlayerVehicle","ReplayMgr",
        "RoomMgr","CarMgr","CarInfo","VehicleInfo","RacingMgr","MatchVideo"
    };
    for(const char* v:want)
        if(strcmp(name,v)==0) return true;
    return false;
}
}

std::string ERIPortCollectSignatures() {
    std::ostringstream out;
    out << "ERI ZSM runtime method survey (read-only)\n";
    out << "No methods were called or patched.\n";
    auto domain=(GDomain)lookup("il2cpp_domain_get");
    auto assemblies=(GAssemblies)lookup("il2cpp_domain_get_assemblies");
    auto image=(GImage)lookup("il2cpp_assembly_get_image");
    auto classCount=(GClassCount)lookup("il2cpp_image_get_class_count");
    auto getClass=(GClass)lookup("il2cpp_image_get_class");
    auto className=(GName)lookup("il2cpp_class_get_name");
    auto classNS=(GName)lookup("il2cpp_class_get_namespace");
    auto classMethods=(GClassMethods)lookup("il2cpp_class_get_methods");
    auto methodName=(GMethodName)lookup("il2cpp_method_get_name");
    auto paramCount=(GParamCount)lookup("il2cpp_method_get_param_count");
    auto classFields=(GFields)lookup("il2cpp_class_get_fields");
    auto fieldName=(GFieldName)lookup("il2cpp_field_get_name");
    auto fieldOffset=(GFieldOffset)lookup("il2cpp_field_get_offset");
    auto current=(ThreadCurrent)lookup("il2cpp_thread_current");
    auto attach=(ThreadAttach)lookup("il2cpp_thread_attach");
    auto detach=(ThreadDetach)lookup("il2cpp_thread_detach");
    if(!domain || !assemblies || !image || !classCount || !getClass || !className ||
       !classMethods || !methodName || !paramCount) {
        out << "Required IL2CPP metadata exports unavailable.\n";
        return out.str();
    }
    void* d=domain();
    if(!d){out << "IL2CPP domain not ready; retry once the game loads.\n";return out.str();}
    void* newThread=nullptr;
    if(current && attach && !current())newThread=attach(d);
    size_t nAssemblies=0;
    const void** list=assemblies(d,&nAssemblies);
    if(!list || nAssemblies>512) {
        out << "Assembly list unavailable.\n";
        if(newThread && detach)detach(newThread);
        return out.str();
    }
    int found=0;
    size_t scanned=0;
    for(size_t ai=0;ai<nAssemblies && found<50 && scanned<160000;ai++) {
        void* img=image(list[ai]);
        if(!img)continue;
        size_t nc=classCount(img);
        if(nc>160000)continue;
        for(size_t j=0;j<nc && found<50 && scanned<160000;j++,scanned++) {
            void* c=getClass(img,j);
            if(!c)continue;
            const char* name=className(c);
            if(!interesting(name))continue;
            found++;
            out << "\n[" << found << "] " << clean(classNS?classNS(c):"") << "."
                << clean(name) << "\n";
            void* mit=nullptr;
            for(int k=0;k<75;k++) {
                const void* method=classMethods(c,&mit);
                if(!method)break;
                const char* mn=methodName(method);
                out << "  method " << clean(mn) << " argc=" << paramCount(method) << "\n";
            }
            if(classFields && fieldName) {
                void* fit=nullptr;
                for(int k=0;k<90;k++) {
                    const void* f=classFields(c,&fit);
                    if(!f)break;
                    out << "  field " << clean(fieldName(f));
                    if(fieldOffset)out << " off=" << fieldOffset(f);
                    out << "\n";
                }
            }
            if(out.tellp()>62000)break;
        }
        if(out.tellp()>62000)break;
    }
    out << "\nSurveyed " << scanned << " classes in " << nAssemblies <<
           " assemblies. Targets found: " << found << ".\n";
    if(!found)out << "This build may not match the legacy ZSM game's class names.\n";
    if(newThread && detach)detach(newThread);
    return out.str();
}
