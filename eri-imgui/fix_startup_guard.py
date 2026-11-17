#!/usr/bin/env python3
"""Avoid eager ImGui/AVGamesMenu startup that can crash an injected Unity IPA.

Apply after fix_overlay.py and fix_runtime_bridge.py.
Only initialize Metal when the app has become active and the launch grace period
has elapsed. Only request original menu rows when ERI overlay is explicitly opened.
"""
from pathlib import Path

source=Path("eri-imgui/Sources/ERIOverlay.mm")
s=source.read_text()

def once(old,new):
    global s
    n=s.count(old)
    if n!=1:
        raise RuntimeError(f"Expected one source match, got {n}: {old[:100]}")
    s=s.replace(old,new,1)

# Constructor no longer calls UIWindow/Metal/ImGui while the game is launching.
start=s.index("__attribute__((constructor)) static void ERIInit(void) {")
end=s.find("\n}",start)
if end<0:
    raise RuntimeError("ERIInit closing brace was not found")
end+=2
tail=s[end:].strip()
if tail:
    raise RuntimeError("Unexpected code after ERIInit; do not remove it")

s=s[:start]+'''// Constructor is allowed to register events but never initializes the
// UI or original game's AVGamesMenu while iOS dyld/startup is still running.
static NSTimeInterval gERIEarliestStart = 1e30;
__attribute__((constructor)) static void ERIInit(void) {
    dispatch_async(dispatch_get_main_queue(), ^{
        UIApplication* app=UIApplication.sharedApplication;
        if(app.applicationState==UIApplicationStateActive)
            gERIEarliestStart=[NSDate timeIntervalSinceReferenceDate]+4.0;
        [[NSNotificationCenter defaultCenter]
          addObserverForName:UIApplicationDidBecomeActiveNotification
          object:nil queue:NSOperationQueue.mainQueue
          usingBlock:^(NSNotification* note) {
            gERIEarliestStart=[NSDate timeIntervalSinceReferenceDate]+4.0;
          }];
        [[NSNotificationCenter defaultCenter]
          addObserverForName:UIApplicationWillResignActiveNotification
          object:nil queue:NSOperationQueue.mainQueue
          usingBlock:^(NSNotification* note) {
            if(!gEriOverlay.view) gERIEarliestStart=1e30;
          }];
        [NSTimer scheduledTimerWithTimeInterval:1.0 repeats:YES
          block:^(NSTimer* timer) {
            if(gEriOverlay.view) { [timer invalidate]; return; }
            if(app.applicationState!=UIApplicationStateActive) return;
            if([NSDate timeIntervalSinceReferenceDate]<gERIEarliestStart) return;
            @try {
                ERITryStart();
            } @catch(NSException* exc) {
                NSLog(@"[ERI ImGui] deferred startup exception: %@",exc.name);
            }
          }];
    });
}
'''
# If the game has not initialized its UI, simply show the ERI floating icon.
# Creating AVGamesMenu.shared on every frame during launch can initialize
# game-owned UIKit controllers before they are ready.
once('    self.open=YES;\n    [self loadOriginalRows];',
     '    self.open=NO;\n    self.bridgeDiagnostic=@"Tap ERI to load the original menu.";')
once('        [self loadOriginalRows];\n        [self suppressOriginalUI];\n        ImGuiIO& io=ImGui::GetIO();',
     '        if(self.open) {\n            [self loadOriginalRows];\n            [self suppressOriginalUI];\n        }\n        ImGuiIO& io=ImGui::GetIO();')
# Avoid running original UI mutation just because the overlay window is active.
once('    if(self.view) { [self suppressOriginalUI]; return; }',
     '    if(self.view) return;')

source.write_text(s)
print("Patched overlay: delayed UIKit startup and click-to-load menu rows")
