// ERIStandalone V5: explicitly requested managed calls through IL2CPP runtime_invoke.
// These calls are NOT replacements for original ZSM car-speed or auto-replay bots.
// They run only when a live typed singleton reference is located at runtime.
#include "ERIRuntimeActions.h"
#include <dlfcn.h>
#include <mach-o/dyld.h>
#include <cstdint>
#include <cstring>
#include <string>
#include <sstream>
#include <cstdio>

namespace {
using Domain = void* (*)();
using Assemblies = const void** (*)(void*,size_t*);
using Image = void* (*)(const void*);
using Count = size_t (*)(void*);
using GetClass = void* (*)(void*,size_t);
using ClassName = const char* (*)(void*);
using Fields = const void* (*)(void*,void**);
using FieldName = const char* (*)(const void*);
using FieldType = const void* (*)(const void*);
using FieldFlags = uint32_t (*)(const void*);
using FieldGetStatic = void (*)(const void*,void*);
using FieldGet = void (*)(void*,const void*,void*);
using ClassFromObj = void* (*)(void*);
using Methods = const void* (*)(void*,void**);
using MethodName = const char* (*)(const void*);
using ParamCount = uint32_t (*)(const void*);
using ParamType = const void* (*)(const void*,uint32_t);
using GetTypeName = char* (*)(const void*);
using Free = void (*)(void*);
using RuntimeInvoke = void* (*)(const void*,void*,void**,void**);
using ObjectUnbox = void* (*)(void*);
using ThreadCurrent = void* (*)();
using ThreadAttach = void* (*)(void*);
using ThreadDetach = void (*)(void*);

std::string message="No runtime actions attempted yet.";
void* resolve(const char* name) {
    if(void* v=dlsym(RTLD_DEFAULT,name))return v;
    for(uint32_t i=0;i<_dyld_image_count();i++) {
        const char* path=_dyld_get_image_name(i);
        if(!path || (!strstr(path,"UnityFramework") && !strstr(path,"GameAssembly")))
            continue;
        void* lib=dlopen(path,RTLD_LAZY|RTLD_NOLOAD);
        if(lib) {
            if(void* v=dlsym(lib,name))return v;
        }
    }
    return nullptr;
}
struct API {
    Domain domain;
    Assemblies assemblies;
    Image image;
    Count classCount;
    GetClass getClass;
    ClassName className;
    Fields fields;
    FieldName fieldName;
    FieldType fieldType;
    FieldFlags fieldFlags;
    FieldGetStatic staticGet;
    FieldGet fieldGet;
    ClassFromObj objectClass;
    Methods methods;
    MethodName methodName;
    ParamCount paramCount;
    ParamType paramType;
    GetTypeName typeName;
    Free freeName;
    RuntimeInvoke invoke;
    ObjectUnbox unbox;
    ThreadCurrent current;
    ThreadAttach attach;
    ThreadDetach detach;
    API():
        domain((Domain)resolve("il2cpp_domain_get")),
        assemblies((Assemblies)resolve("il2cpp_domain_get_assemblies")),
        image((Image)resolve("il2cpp_assembly_get_image")),
        classCount((Count)resolve("il2cpp_image_get_class_count")),
        getClass((GetClass)resolve("il2cpp_image_get_class")),
        className((ClassName)resolve("il2cpp_class_get_name")),
        fields((Fields)resolve("il2cpp_class_get_fields")),
        fieldName((FieldName)resolve("il2cpp_field_get_name")),
        fieldType((FieldType)resolve("il2cpp_field_get_type")),
        fieldFlags((FieldFlags)resolve("il2cpp_field_get_flags")),
        staticGet((FieldGetStatic)resolve("il2cpp_field_static_get_value")),
        fieldGet((FieldGet)resolve("il2cpp_field_get_value")),
        objectClass((ClassFromObj)resolve("il2cpp_object_get_class")),
        methods((Methods)resolve("il2cpp_class_get_methods")),
        methodName((MethodName)resolve("il2cpp_method_get_name")),
        paramCount((ParamCount)resolve("il2cpp_method_get_param_count")),
        paramType((ParamType)resolve("il2cpp_method_get_param")),
        typeName((GetTypeName)resolve("il2cpp_type_get_name")),
        freeName((Free)resolve("il2cpp_free")),
        invoke((RuntimeInvoke)resolve("il2cpp_runtime_invoke")),
        unbox((ObjectUnbox)resolve("il2cpp_object_unbox")),
        current((ThreadCurrent)resolve("il2cpp_thread_current")),
        attach((ThreadAttach)resolve("il2cpp_thread_attach")),
        detach((ThreadDetach)resolve("il2cpp_thread_detach")) {}
    bool ready() const {
        return domain && assemblies && image && classCount && getClass &&
            className && fields && fieldType && fieldFlags && staticGet &&
            objectClass && methods && methodName && paramCount && paramType &&
            typeName && freeName && invoke;
    }
};
bool equals(const char* a,const char* b) {
    return a && b && strcmp(a,b)==0;
}
bool ends(const std::string& s,const char* suffix) {
    const std::string tail(suffix);
    return s.size()>=tail.size() && s.compare(s.size()-tail.size(),tail.size(),tail)==0;
}
std::string typeName(const API& a,const void* type) {
    if(!type)return "";
    char* p=a.typeName(type);
    if(!p)return "";
    std::string s(p);
    a.freeName(p);
    return s;
}
struct Attach {
    const API& a;
    void* thread=nullptr;
    explicit Attach(const API& x,void* d):a(x) {
        if(a.current && a.attach && !a.current())thread=a.attach(d);
    }
    ~Attach() { if(thread && a.detach)a.detach(thread); }
};
struct Target {
    void* klass=nullptr;
    void* instance=nullptr;
    size_t scanned=0;
};

// Read only a *static reference* whose declared managed type matches target.
// Does not guess instance pointer offsets, walk arbitrary heaps or search memory.
Target findTypedStaticManager(const API& a,void* domain,const char* target) {
    Target found;
    size_t na=0;
    const void** asms=a.assemblies(domain,&na);
    if(!asms || na>512)return found;
    for(size_t i=0;i<na && found.scanned<170000;i++) {
        void* img=a.image(asms[i]);
        if(!img)continue;
        size_t n=a.classCount(img);
        if(n>170000)continue;
        for(size_t j=0;j<n && found.scanned<170000;j++,found.scanned++) {
            void* klass=a.getClass(img,j);
            if(!klass)continue;
            if(equals(a.className(klass),target))
                found.klass=klass;
            void* iter=nullptr;
            for(size_t k=0;k<150;k++) {
                const void* field=a.fields(klass,&iter);
                if(!field)break;
                if(!(a.fieldFlags(field)&0x10))continue; // static
                std::string ty=typeName(a,a.fieldType(field));
                if(ty!=target && !ends(ty,(std::string(".")+target).c_str()))
                    continue;
                // Field is declared as a reference type exactly matching target.
                void* object=nullptr;
                a.staticGet(field,&object);
                if(object && equals(a.className(a.objectClass(object)),target)) {
                    found.instance=object;
                    found.klass=a.objectClass(object);
                    return found;
                }
            }
        }
    }
    return found;
}
bool checkTypes(const API& a,const void* method,const char* const* expected,int argc) {
    if((int)a.paramCount(method)!=argc)return false;
    for(int p=0;p<argc;p++) {
        const std::string s=typeName(a,a.paramType(method,(uint32_t)p));
        if(s!=expected[p] && !ends(s,(std::string(".")+expected[p]).c_str()))
            return false;
    }
    return true;
}
const void* method(const API& a,void* klass,const char* name,
                   const char* const* types,int argc) {
    if(!klass)return nullptr;
    void* iter=nullptr;
    for(int i=0;i<350;i++) {
        const void* m=a.methods(klass,&iter);
        if(!m)break;
        if(equals(a.methodName(m),name) && checkTypes(a,m,types,argc))
            return m;
    }
    return nullptr;
}
bool run(const API& a,void* domain,const char* cls,
         const char* name,const char* const* types,int argc,void** args,
         void** returnValue=nullptr) {
    Attach at(a,domain);
    Target manager=findTypedStaticManager(a,domain,cls);
    if(!manager.instance) {
        message=std::string("No live ")+cls+
            " singleton in a strongly-typed static field. No method was called.";
        return false;
    }
    const void* m=method(a,manager.klass,name,types,argc);
    if(!m) {
        message=std::string("Typed method ")+cls+"."+name+" unavailable.";
        return false;
    }
    void* exception=nullptr;
    void* result=a.invoke(m,manager.instance,args,&exception);
    if(exception) {
        message=std::string(cls)+"."+name+" returned a managed exception.";
        return false;
    }
    if(returnValue)*returnValue=result;
    message=std::string("Managed runtime_invoked ")+cls+"."+name+
            "; game-side outcome is not yet verified.";
    return true;
}
bool prepare(API& api,void*& domain) {
    if(!api.ready()) {
        message="Missing IL2CPP runtime metadata/invoke export; no action taken.";
        return false;
    }
    domain=api.domain();
    if(!domain) {
        message="IL2CPP domain not initialized; retry after loading the game.";
        return false;
    }
    return true;
}
}

std::string ERIV5RuntimeStatus(){return message;}

bool ERIV5ProbeVehicle() {
    API a;void* d=nullptr;
    if(!prepare(a,d))return false;
    void* result=nullptr;
    if(!run(a,d,"VehicleMgr","GetMyVehicle",nullptr,0,nullptr,&result))
        return false;
    message=result ? "VehicleMgr.GetMyVehicle() returned a vehicle (read-only)."
                   : "VehicleMgr.GetMyVehicle() returned null.";
    return result!=nullptr;
}
bool ERIV5PreviewGarageVehicle(uint32_t vehicleID) {
    if(!vehicleID) {
        message="Vehicle ID must be positive. No action taken.";return false;
    }
    API a;void* d=nullptr;
    if(!prepare(a,d))return false;
    const char* types[]={"System.UInt32"};
    void* args[]={&vehicleID};
    bool ok=run(a,d,"GarageMgr","SetGarageListShowVehicleID",types,1,args);
    if(ok)message="Requested garage preview by ID. Does not grant ownership or unlock skins.";
    return ok;
}
bool ERIV5QuickMatch(int32_t mode,uint8_t players,int32_t mapID,int32_t useMap) {
    if(mode<0||mode>100||players<1||players>8||mapID<0||useMap<0||useMap>100) {
        message="Match settings out of accepted test range. No match requested.";
        return false;
    }
    API a;void* d=nullptr;
    if(!prepare(a,d))return false;
    // Only perform a match when the game reports it is not already searching.
    const char* noargs[1]={};
    void* stateResult=nullptr;
    if(!run(a,d,"QuickMatchMdl","get_IsMatching",noargs,0,nullptr,&stateResult))
        return false;
    if(!stateResult||!a.unbox) {
        message="Cannot read QuickMatchMdl state; no match requested.";
        return false;
    }
    const bool matching=*(const uint8_t*)a.unbox(stateResult)!=0;
    if(matching) {message="Game already matching; no duplicate request.";return false;}
    bool room=false;
    const char* types[]={"CSRes.ENMClientGameModeType","System.Boolean",
                         "System.Byte","System.Int32","MatchUseMapType"};
    void* args[]={&mode,&room,&players,&mapID,&useMap};
    const bool ok=run(a,d,"QuickMatchMdl","QuickMatch",types,5,args);
    if(ok)message="QuickMatch invoked with entered settings; server acceptance not verified.";
    return ok;
}
bool ERIV5ReplayWaitingShadow() {
    API a;void* d=nullptr;
    if(!prepare(a,d))return false;
    Attach at(a,d);
    Target target=findTypedStaticManager(a,d,"MatchVideoMdl");
    if(!target.instance) {
        message="No live MatchVideoMdl strongly-typed singleton found.";
        return false;
    }
    if(!a.fieldGet || !a.fieldName) {
        message="Cannot read validated waiting replay fields.";
        return false;
    }
    const void* markField=nullptr;
    const void* uidField=nullptr;
    void* iter=nullptr;
    for(int i=0;i<200;i++) {
        const void* f=a.fields(target.klass,&iter);
        if(!f)break;
        if(equals(a.fieldName(f),"<WaitingVideoMark>k__BackingField"))
            markField=f;
        if(equals(a.fieldName(f),"<WaitingShadowUID>k__BackingField"))
            uidField=f;
    }
    if(!markField||!uidField) {
        message="WaitingVideoMark/WaitingShadowUID fields not found.";
        return false;
    }
    int64_t mark=0;uint64_t uid=0;
    a.fieldGet(target.instance,markField,&mark);
    a.fieldGet(target.instance,uidField,&uid);
    if(mark<=0||uid==0) {
        message="No valid waiting replay mark/shadow UID; no challenge sent.";
        return false;
    }
    const char* types[]={"System.Int64","System.UInt64"};
    const void* m=method(a,target.klass,"ChallengeShadow",types,2);
    if(!m) {message="ChallengeShadow(Int64,UInt64) not available.";return false;}
    void* args[]={&mark,&uid};
    void* exception=nullptr;
    void* returnVal=a.invoke(m,target.instance,args,&exception);
    if(exception) {
        message="ChallengeShadow returned managed exception.";
        return false;
    }
    if(returnVal && a.unbox) {
        int32_t status=*(const int32_t*)a.unbox(returnVal);
        message="ChallengeShadow invoked with waiting replay; return code="+
            std::to_string(status)+". This is not the original replay bot.";
    } else message="ChallengeShadow invoked; return code unavailable.";
    return true;
}
