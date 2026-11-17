#!/usr/bin/env python3
from pathlib import Path
p=Path("standalone/Sources/ERIStandalone.mm")
s=p.read_text()
def replace(before,after):
    global s
    n=s.count(before)
    if n!=1:
        raise RuntimeError(f"Expected 1 match, got {n}: {before[:100]}")
    s=s.replace(before,after,1)

replace('#include <algorithm>\n','#include <algorithm>\n#include "ERIGameRuntime.h"\n')
replace('@property(nonatomic,assign) double currentFPS;\n',
'''@property(nonatomic,assign) double currentFPS;
@property(nonatomic,assign) BOOL worldTimeEnabled;
@property(nonatomic,assign) float worldTimeMultiplier;
@property(nonatomic,assign) double lastWorldTimeApply;
@property(nonatomic,assign) double lastRuntimeProbe;
''')
replace('    [self loadSettings];\n','''    [self loadSettings];
    self.worldTimeEnabled=NO; // Never persist gameplay modifications between launches.
    self.worldTimeMultiplier=1.15f;
''')
replace('        self.currentFPS=self.currentFPS*.90 + .10/delta;\n',
'''        self.currentFPS=self.currentFPS*.90 + .10/delta;
        if(tick-self.lastRuntimeProbe>2.0) {
            ERIGameProbe(false); // Wait for Unity to load, no memory scanning here.
            self.lastRuntimeProbe=tick;
        }
        if(self.worldTimeEnabled && tick-self.lastWorldTimeApply>.5) {
            if(!ERIWorldTimeSet(self.worldTimeMultiplier)) self.worldTimeEnabled=NO;
            self.lastWorldTimeApply=tick;
        }
''')
replace('''                    if(ImGui::BeginTabItem("Tinh nang game")) {
                        ImGui::TextWrapped("Chua chuyen hook/tinh nang game vao ban doc lap nay.");
                        ImGui::Separator();
                        ImGui::TextWrapped("ZSM ban cu co logic speed, garage, replay, match va nhieu callback phu thuoc IL2CPP.");
                        ImGui::TextWrapped("Khong hien nut gia khi chua co phan xu ly thuc su.");
                        ImGui::EndTabItem();
                    }
''',
'''                    if(ImGui::BeginTabItem("Tinh nang game")) {
                        const ERIGameRuntimeInfo& game=ERIGameInfo();
                        ImGui::Text("IL2CPP: %s",game.il2cppLoaded?"loaded":"unavailable");
                        ImGui::TextWrapped("%s",game.detail.c_str());
                        if(ImGui::Button("Quet lop IL2CPP (chi doc)")) ERIGameProbe(true);
                        if(game.scannedClasses) {
                            ImGui::Text("Lop da quet: %d | Assembly: %d",game.classesScanned,game.assemblies);
                            ImGui::Text("VehicleMgr: %s",game.vehicleMgrFound?"tim thay":"chua xac dinh");
                            ImGui::Text("GarageMgr: %s",game.garageMgrFound?"tim thay":"chua xac dinh");
                            ImGui::Text("MatchMgr: %s",game.matchMgrFound?"tim thay":"chua xac dinh");
                            ImGui::Text("MatchVideoMdl: %s",game.replayMgrFound?"tim thay":"chua xac dinh");
                        }
                        ImGui::Separator();
                        ImGui::TextWrapped("THU NGHIEM: Thay doi Unity Time.timeScale, KHONG PHAI toc do xe ZSM.");
                        if(game.timeScaleReady) {
                            bool on=self.worldTimeEnabled;
                            if(ImGui::Checkbox("Bat thay doi thoi gian Unity",&on)) {
                                if(on) self.worldTimeEnabled=ERIWorldTimeSet(self.worldTimeMultiplier);
                                else { ERIWorldTimeRestore(); self.worldTimeEnabled=NO; }
                            }
                            float multiplier=self.worldTimeMultiplier;
                            if(ImGui::SliderFloat("TimeScale",&multiplier,.5f,2.0f,"x%.2f")) {
                                self.worldTimeMultiplier=multiplier;
                                if(self.worldTimeEnabled) ERIWorldTimeSet(multiplier);
                            }
                            if(ImGui::Button("Khoi phuc toc do thoi gian")) {
                                ERIWorldTimeRestore();
                                self.worldTimeEnabled=NO;
                            }
                        } else {
                            ImGui::TextDisabled("Unity Time icall not available: no speed control.");
                        }
                        ImGui::Separator();
                        ImGui::TextWrapped("Xe/skin, replay bot va tu ghep tran CHUA port hook.");
                        ImGui::TextWrapped("Can game-specific method bindings duoc xac minh, khong tao nut gia.");
                        ImGui::EndTabItem();
                    }
''')
replace('''        [[NSNotificationCenter defaultCenter] addObserverForName:UIApplicationDidBecomeActiveNotification
''',
'''        [[NSNotificationCenter defaultCenter] addObserverForName:UIApplicationWillResignActiveNotification
         object:nil queue:NSOperationQueue.mainQueue usingBlock:^(NSNotification* note) {
            ERIWorldTimeRestore();
            if(gERIStandalone) gERIStandalone.worldTimeEnabled=NO;
        }];
        [[NSNotificationCenter defaultCenter] addObserverForName:UIApplicationDidBecomeActiveNotification
''')
p.write_text(s)
print("Applied self-contained IL2CPP probe + optional Unity timeScale controls")
