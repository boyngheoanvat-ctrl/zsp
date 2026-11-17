// ERIStandalone V4: typed survey for exact ZSM hook candidates.
// No method invocation, raw memory writes, trampolines, or old dylib dependencies.
#include "ERITypedTargetReport.h"
#include <dlfcn.h>
#include <mach-o/dyld.h>
#include <cstring>
#include <cstdint>
#include <sstream>
#include <string>
#include <vector>

namespace {
using Domain = void* (*)();
using Assemblies = const void** (*)(void*,size_t*);
using Image = void* (*)(const void*);
using Count = size_t (*)(void*);
using GetClass = void* (*)(void*,size_t);
using ClassName = const char* (*)(void*);
using Methods = const void* (*)(void*,void**);
using MethodName = const char* (*)(const void*);
using ParamCount = uint32_t (*)(const void*);
using ParamType = const void* (*)(const void*,uint32_t);
using ParamName = const char* (*)(const void*,uint32_t);
using ReturnType = const void* (*)(const void*);
using Flags = uint32_t (*)(const void*,uint32_t*);
using TypeName = char* (*)(const void*);
using Free = void (*)(void*);
using Fields = const void* (*)(void*,void**);
using FieldName = const char* (*)(const void*);
using FieldType = const void* (*)(const void*);
using FieldOffset = int (*)(const void*);
using ThreadCurrent = void* (*)();
using ThreadAttach = void* (*)(void*);
using ThreadDetach = void (*)(void*);

void* sym(const char* name) {
    if(void* v=dlsym(RTLD_DEFAULT,name))return v;
    for(uint32_t i=0;i<_dyld_image_count();i++) {
        const char* p=_dyld_get_image_name(i);
        if(!p || (!strstr(p,"UnityFramework") && !strstr(p,"GameAssembly")))continue;
        void* lib=dlopen(p,RTLD_LAZY|RTLD_NOLOAD);
        if(lib)if(void* v=dlsym(lib,name))return v;
    }
    return nullptr;
}
struct Target {
    const char* cls;
    const char* group;
    const char* methods[11];
    const char* fields[7];
};
constexpr Target targets[] = {
    {"VehicleMgr","VEHICLE SPEED",
     {"GetMyVehicle","GetRaceCarParams","RefreshVehicleParam","RefreshVehicleParams",
      "Update","GetDefaultVehiclePrefabParams","GetVehicleByID",nullptr},
     {"mMyVehicle","mPhyEngine","mVehicleList","mMatchMgr",nullptr}},
    {"GarageMgr","GARAGE AND SKINS",
     {"SetGarageShowBySkinData","SetGarageListShowVehicleID","OnChangeVehicle",
      "GoToOwnedItemById","SetGarageShowList","CreatePresentationVehicle",nullptr},
     {"mGarageSystem","mGarageListSystem","mVehicleCustomComponent",nullptr}},
    {"MatchVideoMdl","REPLAY BOT",
     {"ReplayVideo","ChallengeShadow","OnlineShadowChallenge",
      "StartEnterGame","get_CurReplayVideo","get_MatchVideoMgr",
      "SetWaitingVideoInfo","ReqStartChallengeShadowOnlineGame",nullptr},
     {"<CurReplayVideo>k__BackingField","<MatchVideoMgr>k__BackingField",
      "<WaitingVideoMark>k__BackingField",nullptr}},
    {"QuickMatchMdl","AUTO MATCH",
     {"QuickMatch","PreJoinMatch","get_IsMatching","get_MatchType",
      "CancelMatch","SetSuccess","ResetQuickMatchState",nullptr},
     {"mQuickMatchComp","mIsInit",nullptr}},
    {"MatchMgr","MATCH STATE",
     {"get_CurMatch","GetInGameState","GetGameType","OnGameEnter","OnGameExit",nullptr},
     {"<CurMatch>k__BackingField","mNetModule",nullptr}},
    {"VehicleInfo","VEHICLE DATA",
     {nullptr},
     {"stVehicleInfo","stTireMarkInfo","stDrifrFlameInfo",nullptr}}
};
bool has(const char* s,const char* const* items) {
    if(!s)return false;
    for(size_t i=0;i<11 && items[i];i++)
        if(strcmp(s,items[i])==0)return true;
    return false;
}
std::string cleaned(const char* s) {
    if(!s)return "<null>";
    std::string result;
    for(size_t i=0;i<180 && s[i];i++) {
        char c=s[i];
        result.push_back(c=='\n'||c=='\r'?'_':c);
    }
    return result;
}
std::string managedType(const void* type,TypeName name,Free release) {
    if(!type)return "void/unknown";
    if(!name || !release)return "<il2cpp_type_get_name unavailable>";
    char* str=name(type);
    if(!str)return "<unknown type>";
    std::string result=cleaned(str);
    release(str);
    return result;
}
}
std::string ERITypedTargetReport() {
    std::ostringstream out;
    out << "ERI ZSM typed hook candidate survey (READ-ONLY V4)\n";
    out << "No gameplay methods were called or patched.\n";
    out << "A matching class or method is NOT proof a gameplay hook is active.\n";
    auto domain=(Domain)sym("il2cpp_domain_get");
    auto assemblies=(Assemblies)sym("il2cpp_domain_get_assemblies");
    auto image=(Image)sym("il2cpp_assembly_get_image");
    auto numClasses=(Count)sym("il2cpp_image_get_class_count");
    auto getClass=(GetClass)sym("il2cpp_image_get_class");
    auto className=(ClassName)sym("il2cpp_class_get_name");
    auto classNamespace=(ClassName)sym("il2cpp_class_get_namespace");
    auto methods=(Methods)sym("il2cpp_class_get_methods");
    auto methodName=(MethodName)sym("il2cpp_method_get_name");
    auto paramCount=(ParamCount)sym("il2cpp_method_get_param_count");
    auto paramType=(ParamType)sym("il2cpp_method_get_param");
    auto paramName=(ParamName)sym("il2cpp_method_get_param_name");
    auto returnType=(ReturnType)sym("il2cpp_method_get_return_type");
    auto flags=(Flags)sym("il2cpp_method_get_flags");
    auto typeName=(TypeName)sym("il2cpp_type_get_name");
    auto release=(Free)sym("il2cpp_free");
    auto fields=(Fields)sym("il2cpp_class_get_fields");
    auto fieldName=(FieldName)sym("il2cpp_field_get_name");
    auto fieldType=(FieldType)sym("il2cpp_field_get_type");
    auto fieldOffset=(FieldOffset)sym("il2cpp_field_get_offset");
    auto current=(ThreadCurrent)sym("il2cpp_thread_current");
    auto attach=(ThreadAttach)sym("il2cpp_thread_attach");
    auto detach=(ThreadDetach)sym("il2cpp_thread_detach");
    if(!domain || !assemblies || !image || !numClasses || !getClass ||
       !className || !methods || !methodName || !paramCount) {
        out << "Required IL2CPP metadata APIs are unavailable.\n";
        return out.str();
    }
    if(!typeName || !release || !paramType || !returnType)
        out << "WARNING: parameter type APIs missing; exact signature unavailable.\n";
    void* d=domain();
    if(!d) {out<<"IL2CPP domain not initialized.\n";return out.str();}
    void* newThread=nullptr;
    if(current && attach && !current()) newThread=attach(d);
    size_t nAssemblies=0;
    const void** list=assemblies(d,&nAssemblies);
    if(!list || nAssemblies>512) {
        if(newThread && detach)detach(newThread);
        out << "Assembly list not available.\n";return out.str();
    }
    int found=0;
    size_t scanned=0;
    for(size_t ai=0;ai<nAssemblies && scanned<160000;ai++) {
        void* img=image(list[ai]);
        if(!img)continue;
        size_t n=numClasses(img);
        if(n>160000)continue;
        for(size_t ci=0;ci<n && scanned<160000;ci++,scanned++) {
            void* c=getClass(img,ci);
            if(!c)continue;
            const char* classStr=className(c);
            for(const auto& t:targets) {
                if(!classStr || strcmp(t.cls,classStr)!=0)continue;
                found++;
                out << "\n=== "<<t.group<<" :: "<<
                    cleaned(classNamespace?classNamespace(c):"")<<
                    "."<<cleaned(classStr)<<" ===\n";
                void* iter=nullptr;
                for(int mi=0;mi<360;mi++) {
                    const void* m=methods(c,&iter);
                    if(!m)break;
                    const char* n=methodName(m);
                    if(!has(n,t.methods))continue;
                    uint32_t argc=paramCount(m);
                    uint32_t iflags=0;
                    uint32_t mf=flags?flags(m,&iflags):0;
                    out << "  "<<((mf&0x0010)?"STATIC ":"INSTANCE ")
                        <<managedType(returnType?returnType(m):nullptr,typeName,release)
                        <<" "<<cleaned(n)<<"(";
                    for(uint32_t pi=0;pi<argc && pi<24;pi++) {
                        if(pi)out<<", ";
                        const void* ty=paramType?paramType(m,pi):nullptr;
                        out<<managedType(ty,typeName,release)<<" ";
                        out<<cleaned(paramName?paramName(m,pi):nullptr);
                    }
                    if(argc>24)out<<", ...";
                    out<<") [argc="<<argc<<"]\n";
                }
                if(fields && fieldName) {
                    void* fieldIter=nullptr;
                    for(int fi=0;fi<250;fi++) {
                        const void* field=fields(c,&fieldIter);
                        if(!field)break;
                        const char* n=fieldName(field);
                        if(!has(n,t.fields))continue;
                        out<<"  field "<<managedType(fieldType?fieldType(field):nullptr,typeName,release)
                           <<" "<<cleaned(n)<<" @"
                           <<(fieldOffset?fieldOffset(field):-1)<<"\n";
                    }
                }
            }
            if(out.tellp()>54000)break;
        }
        if(out.tellp()>54000)break;
    }
    out<<"\nAssemblies="<<nAssemblies<<" Scanned="<<scanned
       <<" CandidateClasses="<<found<<"\n";
    out<<"NEXT: validate exact types + object lifetime + semantic side effects;\n";
    out<<"do not use argc or offsets alone for native function invocation.\n";
    if(newThread && detach)detach(newThread);
    return out.str();
}
