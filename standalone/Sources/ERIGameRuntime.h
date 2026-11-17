#pragma once
#include <string>
struct ERIGameRuntimeInfo {
    bool il2cppLoaded=false;
    bool timeScaleReady=false;
    bool scannedClasses=false;
    bool vehicleMgrFound=false;
    bool garageMgrFound=false;
    bool matchMgrFound=false;
    bool replayMgrFound=false;
    int assemblies=0;
    int classesScanned=0;
    std::string detail;
};
void ERIGameProbe(bool scanClasses);
const ERIGameRuntimeInfo& ERIGameInfo();
bool ERIWorldTimeSet(float multiplier);
void ERIWorldTimeRestore();
bool ERIWorldTimeActive();
