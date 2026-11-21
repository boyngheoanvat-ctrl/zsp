// ERI ZSM Android ARM64 v0.1 - Runtime bridge for IL2CPP and an in-memory Android UI.
// Refuses to call unverified methods; no raw offsets or iOS Mach-O hooks are reused.
#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <pthread.h>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <mutex>
#include <atomic>
#include <string>
#include <thread>
#include <chrono>
#include "eri_ui_dex.h"

namespace {
JavaVM* gVM=nullptr;
std::atomic<bool> started{false};
std::mutex lock;
std::string message="ERI native Android v0.1 | chua kiem tra IL2CPP";
void report(const std::string& s){
  __android_log_print(ANDROID_LOG_INFO,"ERI_ZSM","%s",s.c_str());
  std::lock_guard<std::mutex> guard(lock);message=s;
}
std::string current(){std::lock_guard<std::mutex> guard(lock);return message;}
void clear(JNIEnv* e,const char* name){
  if(e->ExceptionCheck()){e->ExceptionClear();__android_log_print(ANDROID_LOG_WARN,"ERI_ZSM","JNI: %s",name);}
}
using Dom=void*(*)();
using Asms=const void**(*)(void*,size_t*);
using Img=void*(*)(const void*);
using Cnt=size_t(*)(void*);
using Cls=void*(*)(void*,size_t);
using Name=const char*(*)(void*);
using Flds=const void*(*)(void*,void**);
using FName=const char*(*)(const void*);
using FType=const void*(*)(const void*);
using FFlag=uint32_t(*)(const void*);
using StaticGet=void(*)(const void*,void*);
using FieldGet=void(*)(void*,const void*,void*);
using ObjClass=void*(*)(void*);
using Meth=const void*(*)(void*,void**);
using MName=const char*(*)(const void*);
using MCount=uint32_t(*)(const void*);
using MParam=const void*(*)(const void*,uint32_t);
using TyName=char*(*)(const void*);
using Free=void(*)(void*);
using Invoke=void*(*)(const void*,void*,void**,void**);
using Unbox=void*(*)(void*);
using Current=void*(*)();
using Attach=void*(*)(void*);
using Detach=void(*)(void*);
struct Api {
 void* lib=nullptr;
 Dom domain=nullptr;Asms assemblies=nullptr;Img image=nullptr;Cnt count=nullptr;
 Cls cls=nullptr;Name name=nullptr;Flds fields=nullptr;FName fieldname=nullptr;
 FType fieldtype=nullptr;FFlag flags=nullptr;StaticGet staticget=nullptr;
 FieldGet fieldget=nullptr;ObjClass objectclass=nullptr;Meth methods=nullptr;
 MName methodname=nullptr;MCount argc=nullptr;MParam param=nullptr;
 TyName typename_=nullptr;Free freename=nullptr;Invoke invoke=nullptr;
 Unbox unbox=nullptr;Current threadcurrent=nullptr;Attach threadattach=nullptr;
 Detach threaddetach=nullptr;
 Api(){
  lib=dlopen("libil2cpp.so",RTLD_NOLOAD|RTLD_NOW);
  if(!lib)return;
#define LOAD(field,name) field=(decltype(field))dlsym(lib,"il2cpp_" name)
  LOAD(domain,"domain_get");LOAD(assemblies,"domain_get_assemblies");
  LOAD(image,"assembly_get_image");LOAD(count,"image_get_class_count");
  LOAD(cls,"image_get_class");LOAD(name,"class_get_name");
  LOAD(fields,"class_get_fields");LOAD(fieldname,"field_get_name");
  LOAD(fieldtype,"field_get_type");LOAD(flags,"field_get_flags");
  LOAD(staticget,"field_static_get_value");LOAD(fieldget,"field_get_value");
  LOAD(objectclass,"object_get_class");LOAD(methods,"class_get_methods");
  LOAD(methodname,"method_get_name");LOAD(argc,"method_get_param_count");
  LOAD(param,"method_get_param");LOAD(typename_,"type_get_name");
  LOAD(freename,"free");LOAD(invoke,"runtime_invoke");
  LOAD(unbox,"object_unbox");LOAD(threadcurrent,"thread_current");
  LOAD(threadattach,"thread_attach");LOAD(threaddetach,"thread_detach");
#undef LOAD
 }
 ~Api(){if(lib)dlclose(lib);}
 bool ready()const{return domain&&assemblies&&image&&count&&cls&&name&&fields&&
   fieldtype&&flags&&staticget&&objectclass&&methods&&methodname&&argc&&param&&
   typename_&&freename&&invoke&&threadattach;}
};
struct ScopedThread {
 Api& a;void* handle=nullptr;
 ScopedThread(Api& api,void* d):a(api){if(!a.threadcurrent||!a.threadcurrent())handle=a.threadattach(d);}
 ~ScopedThread(){if(handle&&a.threaddetach)a.threaddetach(handle);}
};
bool eq(const char* a,const char* b){return a&&b&&strcmp(a,b)==0;}
bool ends(const std::string& s,const std::string& t){
 return s.size()>=t.size()&&s.compare(s.size()-t.size(),t.size(),t)==0;
}
std::string type(Api& a,const void* t){
 if(!t)return "";char* p=a.typename_(t);if(!p)return "";
 std::string result(p);a.freename(p);return result;
}
struct Target{void* cls=nullptr;void* obj=nullptr;size_t scanned=0;};
Target manager(Api& a,void* d,const char* n){
 Target t;size_t na=0;const void** asms=a.assemblies(d,&na);
 if(!asms||na>512)return t;
 for(size_t i=0;i<na&&t.scanned<160000;i++){
  void* im=a.image(asms[i]);if(!im)continue;
  size_t nc=a.count(im);if(nc>160000)continue;
  for(size_t j=0;j<nc&&t.scanned<160000;j++,t.scanned++){
   void* c=a.cls(im,j);if(!c)continue;
   if(eq(a.name(c),n))t.cls=c;
   void* it=nullptr;
   for(int k=0;k<180;k++){
    const void* f=a.fields(c,&it);if(!f)break;
    if(!(a.flags(f)&0x10))continue;
    const std::string ty=type(a,a.fieldtype(f));
    if(ty!=n&&!ends(ty,std::string(".")+n))continue;
    void* ptr=nullptr;a.staticget(f,&ptr);if(!ptr)continue;
    void* actual=a.objectclass(ptr);
    if(actual&&eq(a.name(actual),n)){t.obj=ptr;t.cls=actual;return t;}
   }
  }
 }
 return t;
}
const void* method(Api& a,void* c,const char* n,const char* const* params,int count){
 if(!c)return nullptr;void* it=nullptr;
 for(int i=0;i<400;i++){
  const void* m=a.methods(c,&it);if(!m)break;
  if(!eq(a.methodname(m),n)||(int)a.argc(m)!=count)continue;
  bool ok=true;
  for(int j=0;j<count;j++){
   std::string ty=type(a,a.param(m,j));
   if(ty!=params[j]&&!ends(ty,std::string(".")+params[j])){ok=false;break;}
  }
  if(ok)return m;
 }
 return nullptr;
}
bool run(Api& a,Target& t,const char* klass,const char* name,
         const char* const* params,int count,void** args,void** value=nullptr){
 if(!t.obj){report(std::string(klass)+": khong tim thay singleton dang chay.");return false;}
 const void* m=method(a,t.cls,name,params,count);
 if(!m){report(std::string(klass)+"."+name+": sai hoac thieu signature.");return false;}
 void* ex=nullptr;void* result=a.invoke(m,t.obj,args,&ex);
 if(ex){report(std::string(klass)+"."+name+": managed exception.");return false;}
 if(value)*value=result;return true;
}
void action(int cmd,int x,int y,int z,int w){
 Api a;if(!a.ready()){report("IL2CPP chua nap hoac thieu API, thu lai sau.");return;}
 void* d=a.domain();if(!d){report("IL2CPP domain chua khoi tao.");return;}
 ScopedThread guard(a,d);
 if(cmd==0){
  size_t n=0;const void** asms=a.assemblies(d,&n);
  if(!asms||n>512){report("Khong doc duoc assemblies.");return;}
  size_t classes=0;int found=0;
  const char* wanted[]={"VehicleMgr","GarageMgr","QuickMatchMdl","MatchVideoMdl"};
  for(size_t i=0;i<n;i++){
   void* im=a.image(asms[i]);if(!im)continue;
   size_t nc=a.count(im);if(nc>160000)continue;classes+=nc;
   for(size_t j=0;j<nc;j++){
    void* c=a.cls(im,j);if(!c)continue;
    for(const char* n:wanted)if(eq(a.name(c),n)){found++;break;}
   }
  }
  char b[256];snprintf(b,sizeof(b),"IL2CPP: %zu assemblies, %zu classes, %d class phu hop",n,classes,found);
  report(b);return;
 }
 const char* klass=cmd==1?"VehicleMgr":cmd==2?"GarageMgr":
      cmd==3?"QuickMatchMdl":cmd==4?"MatchVideoMdl":"";
 if(!*klass){report("Lenh chua duoc ho tro.");return;}
 Target t=manager(a,d,klass);
 if(!t.cls){report(std::string(klass)+": class khong tim thay.");return;}
 if(!t.obj){report(std::string(klass)+": class co nhung chua tim thay instance.");return;}
 if(cmd==1){
  void* value=nullptr;
  if(run(a,t,klass,"GetMyVehicle",nullptr,0,nullptr,&value))
   report(value?"Da lay duoc xe hien tai (chi doc).":"GetMyVehicle() tra ve null.");
 }else if(cmd==2){
  if(x<=0){report("Vehicle ID phai >0.");return;}
  uint32_t id=(uint32_t)x;void* args[]={&id};
  const char* types[]={"System.UInt32"};
  if(run(a,t,klass,"SetGarageListShowVehicleID",types,1,args))
   report("Da goi garage preview; chua xac minh ket qua giao dien.");
 }else if(cmd==3){
  if(x<0||x>100||y<0||z<1||z>8||w<0||w>100){
   report("Gia tri quick match khong hop le.");return;
  }
  void* state=nullptr;
  if(!run(a,t,klass,"get_IsMatching",nullptr,0,nullptr,&state))return;
  if(!state||!a.unbox){report("Khong doc duoc isMatching.");return;}
  if(*(uint8_t*)a.unbox(state)){report("Game dang matching, khong goi lai.");return;}
  int32_t mode=x,map=y,use=w;uint8_t players=(uint8_t)z;bool room=false;
  void* args[]={&mode,&room,&players,&map,&use};
  const char* types[]={"CSRes.ENMClientGameModeType","System.Boolean","System.Byte",
                      "System.Int32","MatchUseMapType"};
  if(run(a,t,klass,"QuickMatch",types,5,args))
   report("Da goi QuickMatch mot lan; server result chua duoc xac nhan.");
 }else if(cmd==4){
  if(!a.fieldget||!a.fieldname){report("Missing field APIs.");return;}
  const void* markField=nullptr;const void* uidField=nullptr;void* iter=nullptr;
  for(int i=0;i<256;i++){
   const void* f=a.fields(t.cls,&iter);if(!f)break;
   if(eq(a.fieldname(f),"<WaitingVideoMark>k__BackingField"))markField=f;
   if(eq(a.fieldname(f),"<WaitingShadowUID>k__BackingField"))uidField=f;
  }
  if(!markField||!uidField){report("Khong tim thay waiting replay fields.");return;}
  int64_t mark=0;uint64_t uid=0;
  a.fieldget(t.obj,markField,&mark);a.fieldget(t.obj,uidField,&uid);
  if(mark<=0||uid==0){report("Khong co waiting replay hop le.");return;}
  const char* types[]={"System.Int64","System.UInt64"};
  void* args[]={&mark,&uid};
  if(run(a,t,klass,"ChallengeShadow",types,2,args))
   report("Da goi ChallengeShadow; chua phai replay bot tu dong.");
 }
}
jstring JNICALL jStatus(JNIEnv* e,jclass){
 std::string v=current();return e->NewStringUTF(v.c_str());
}
void JNICALL jCommand(JNIEnv*,jclass,jint cmd,jint a,jint b,jint c,jint d){
 action(cmd,a,b,c,d);
}
jobject findActivity(JNIEnv* e){
 jclass th=e->FindClass("android/app/ActivityThread");
 if(!th){clear(e,"ActivityThread");return nullptr;}
 jmethodID cur=e->GetStaticMethodID(th,"currentApplication","()Landroid/app/Application;");
 if(!cur){clear(e,"currentApplication");return nullptr;}
 jobject app=e->CallStaticObjectMethod(th,cur);
 if(!app){clear(e,"getApplication");return nullptr;}
 jclass obj=e->GetObjectClass(app);
 jmethodID getCL=e->GetMethodID(obj,"getClassLoader","()Ljava/lang/ClassLoader;");
 if(!getCL){clear(e,"getClassLoader");return nullptr;}
 jobject loader=e->CallObjectMethod(app,getCL);
 if(!loader){clear(e,"loader");return nullptr;}
 jclass cl=e->FindClass("java/lang/ClassLoader");
 jmethodID load=e->GetMethodID(cl,"loadClass","(Ljava/lang/String;)Ljava/lang/Class;");
 jstring name=e->NewStringUTF("com.unity3d.player.UnityPlayer");
 jobject uc=e->CallObjectMethod(loader,load,name);e->DeleteLocalRef(name);
 if(!uc){clear(e,"UnityPlayer");return nullptr;}
 jfieldID field=e->GetStaticFieldID((jclass)uc,"currentActivity","Landroid/app/Activity;");
 if(!field){clear(e,"currentActivity");return nullptr;}
 jobject act=e->GetStaticObjectField((jclass)uc,field);
 if(!act)clear(e,"Activity null");
 return act;
}
bool install(JNIEnv* e,jobject activity){
 jclass actCls=e->GetObjectClass(activity);
 jmethodID getCL=e->GetMethodID(actCls,"getClassLoader","()Ljava/lang/ClassLoader;");
 jobject parent=e->CallObjectMethod(activity,getCL);
 if(!parent){clear(e,"parentLoader");return false;}
 jobject buf=e->NewDirectByteBuffer((void*)g_eri_dex,(jlong)g_eri_dex_size);
 if(!buf){clear(e,"ByteBuffer");return false;}
 jclass dexCls=e->FindClass("dalvik/system/InMemoryDexClassLoader");
 if(!dexCls){clear(e,"DexClassLoader API26+");return false;}
 jmethodID ctor=e->GetMethodID(dexCls,"<init>","(Ljava/nio/ByteBuffer;Ljava/lang/ClassLoader;)V");
 if(!ctor){clear(e,"dex ctor");return false;}
 jobject loader=e->NewObject(dexCls,ctor,buf,parent);
 if(!loader){clear(e,"dex instance");return false;}
 jclass cl=e->FindClass("java/lang/ClassLoader");
 jmethodID load=e->GetMethodID(cl,"loadClass","(Ljava/lang/String;)Ljava/lang/Class;");
 jstring name=e->NewStringUTF("com.eri.zsm.EriBridge");
 jobject cls=e->CallObjectMethod(loader,load,name);e->DeleteLocalRef(name);
 if(!cls){clear(e,"EriBridge class");return false;}
 JNINativeMethod methods[]={
  {const_cast<char*>("nativeStatus"),const_cast<char*>("()Ljava/lang/String;"),(void*)jStatus},
  {const_cast<char*>("nativeCommand"),const_cast<char*>("(IIIII)V"),(void*)jCommand}
 };
 if(e->RegisterNatives((jclass)cls,methods,2)!=JNI_OK){
  clear(e,"RegisterNatives");return false;
 }
 jmethodID show=e->GetStaticMethodID((jclass)cls,"install","(Landroid/app/Activity;)V");
 if(!show){clear(e,"Bridge.install");return false;}
 e->CallStaticVoidMethod((jclass)cls,show,activity);
 if(e->ExceptionCheck()){clear(e,"Bridge.install()");return false;}
 report("ERI overlay da yeu cau hien tren Unity Activity.");return true;
}
void* threadmain(void*){
 for(int i=0;i<60;i++){
  std::this_thread::sleep_for(std::chrono::milliseconds(700));
  if(!gVM)continue;
  JNIEnv* e=nullptr;bool attached=false;
  if(gVM->GetEnv((void**)&e,JNI_VERSION_1_6)!=JNI_OK){
   if(gVM->AttachCurrentThread((void**)&e,nullptr)!=JNI_OK)continue;
   attached=true;
  }
  jobject act=findActivity(e);
  if(act){
   if(install(e,act)){
    e->DeleteLocalRef(act);
    if(attached)gVM->DetachCurrentThread();
    return nullptr;
   }
   e->DeleteLocalRef(act);
  }
  if(attached)gVM->DetachCurrentThread();
 }
 report("Khong tim thay Unity Activity hoac UI DEX khong nap duoc.");
 return nullptr;
}
void start(){
 if(started.exchange(true))return;
 pthread_t t;
 if(pthread_create(&t,nullptr,threadmain,nullptr)==0)pthread_detach(t);
 else started=false;
}
} // namespace

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* jvm,void*){
 gVM=jvm;start();return JNI_VERSION_1_6;
}
extern "C" JNIEXPORT void eri_set_java_vm(JavaVM* jvm){gVM=jvm;start();}
extern "C" JNIEXPORT const char* eri_version(){return "ERI ZSM Android v0.1";}