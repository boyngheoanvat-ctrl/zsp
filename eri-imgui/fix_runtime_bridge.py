#!/usr/bin/env python3
"""Fix missing ImGui tabs: resolve the original menu runtime and retry until ready.
Applied AFTER fix_overlay.py unpacks and updates the existing sources.
No fake game callbacks or features are created.
"""
from pathlib import Path

root = Path("eri-imgui/Sources")
overlay = root / "ERIOverlay.mm"
menu_cpp = root / "ERIMenu.cpp"
menu_h = root / "ERIMenu.h"
s = overlay.read_text()
m = menu_cpp.read_text()
h = menu_h.read_text()

def once(text, before, after):
    n = text.count(before)
    if n != 1:
        raise RuntimeError(f"Expected 1 source match, found {n}: {before[:100]}")
    return text.replace(before, after, 1)

s = once(s, "#include <cstdio>", "#include <cstdio>\n#include <dlfcn.h>")
s = once(s,
"""@property(nonatomic,assign) BOOL open;
@end

@implementation ERIOverlay""",
"""@property(nonatomic,assign) BOOL open;
@property(nonatomic,copy) NSString* bridgeDiagnostic;
@property(nonatomic,assign) NSTimeInterval nextBridgeAttempt;
@end

@implementation ERIOverlay""")
start = s.index("- (id)originalMenu {")
end = s.index("- (void)suppressOriginalUI {",start)
s = s[:start] + """- (id)originalMenu {
    // Class does not exist if the IPA contains ONLY ERIImGuiOverlay.dylib.
    Class type=objc_getClass("AVGamesMenu");
    if(!type) {
        static BOOL attemptedDlopen=NO;
        if(!attemptedDlopen) {
            attemptedDlopen=YES;
            NSString* dir=NSBundle.mainBundle.privateFrameworksPath;
            NSString* path=[dir stringByAppendingPathComponent:@"ERI.dylib"];
            if(path && [NSFileManager.defaultManager fileExistsAtPath:path]) {
                // The original dylib must be signed and compatible with this IPA.
                void* handle=dlopen(path.fileSystemRepresentation,RTLD_NOW|RTLD_LOCAL);
                if(!handle) {
                    const char* why=dlerror();
                    self.bridgeDiagnostic=[NSString stringWithFormat:
                        @"ERI.dylib load failed: %s",why?why:"unknown"];
                }
            } else {
                self.bridgeDiagnostic=@"Missing ERI.dylib in App.app/Frameworks. ImGui is a UI-only module.";
            }
        }
        type=objc_getClass("AVGamesMenu");
    }
    if(!type) return nil;
    if(![(id)type respondsToSelector:@selector(shared)]) {
        self.bridgeDiagnostic=@"AVGamesMenu.shared missing. Check original dylib version.";
        return nil;
    }
    @try {
        id instance=((id(*)(id,SEL))objc_msgSend)((id)type,@selector(shared));
        if(!instance) self.bridgeDiagnostic=@"AVGamesMenu.shared returned nil; retrying.";
        return instance;
    } @catch(NSException* exc) {
        self.bridgeDiagnostic=@"AVGamesMenu.shared raised an exception.";
        return nil;
    }
}

""" + s[end:]
start = s.index("- (void)loadOriginalRows {")
end = s.index("- (void)drawRowsForTab:",start)
s = s[:start] + """- (void)loadOriginalRows {
    // Keep successful rows and avoid querying a partially initialized game 60 times/sec.
    if(self.names.count && self.cachedRows.count >= self.names.count) {
        [self suppressOriginalUI];
        return;
    }
    NSTimeInterval t=[NSDate timeIntervalSinceReferenceDate];
    if(t<self.nextBridgeAttempt) return;
    self.nextBridgeAttempt=t+1.0;

    id menu=[self originalMenu];
    if(!menu) return;
    if(![menu respondsToSelector:@selector(segmentTitles)]) {
        self.bridgeDiagnostic=@"Original AVGamesMenu has no segmentTitles selector.";
        return;
    }
    if(![menu respondsToSelector:@selector(rowsForSegment:)]) {
        self.bridgeDiagnostic=@"Original AVGamesMenu has no rowsForSegment selector.";
        return;
    }
    @try {
        id available=[menu segmentTitles];
        if(![available isKindOfClass:NSArray.class]) {
            self.bridgeDiagnostic=@"Original segmentTitles did not return an array.";
            return;
        }
        NSArray* names=(NSArray*)available;
        if(!names.count) {
            self.bridgeDiagnostic=@"Original menu has no tabs yet; retrying after game startup.";
            return;
        }
        for(id name in names) {
            if(![name isKindOfClass:NSString.class]) {
                self.bridgeDiagnostic=@"Original menu returned invalid tab names.";
                return;
            }
        }
        NSMutableDictionary<NSNumber*,NSArray*>* rows=
            self.cachedRows ?: [NSMutableDictionary dictionary];
        for(NSInteger i=0;i<(NSInteger)names.count;i++) {
            // Empty rows are not cached: the game may populate them later.
            if([rows[@(i)] count]) continue;
            id items=[menu rowsForSegment:i];
            if([items isKindOfClass:NSArray.class] && [items count]) {
                rows[@(i)]=items;
            }
        }
        self.names=(NSArray<NSString*>*)names;
        self.cachedRows=rows;
        if(rows.count) {
            self.bridgeDiagnostic=[NSString stringWithFormat:
                 @"Loaded %lu/%lu tabs from ERI.dylib",
                 (unsigned long)rows.count,(unsigned long)names.count];
            [self suppressOriginalUI];
        } else {
            self.bridgeDiagnostic=@"Tabs found, but no row models yet. Retrying...";
        }
    } @catch(NSException* exc) {
        self.bridgeDiagnostic=[NSString stringWithFormat:
            @"Original menu bridge exception: %@",exc.name];
    }
}

""" + s[end:]
s = once(s,
"if(open) ERI_RenderMenu(&open,tabs.empty()?nullptr:tabs.data(),(int)tabs.size());",
"if(open) ERI_RenderMenu(&open,tabs.empty()?nullptr:tabs.data(),(int)tabs.size(),ERIUTF8(self.bridgeDiagnostic).c_str());")
h = once(h,
"void ERI_RenderMenu(bool *isOpen, const ERITab *tabs, int count);",
"void ERI_RenderMenu(bool *isOpen, const ERITab *tabs, int count, const char *diagnostic);")
m = once(m,
"void ERI_RenderMenu(bool *open,const ERITab *tabs,int count) {",
"void ERI_RenderMenu(bool *open,const ERITab *tabs,int count,const char *diagnostic) {")
m = once(m,
'ImGui::TextWrapped("Chua tai duoc danh sach chuc nang. Kiem tra dylib goc.");',
'''ImGui::TextWrapped("Menu bridge: %s", (diagnostic && *diagnostic) ? diagnostic
                               : "Waiting for ERI.dylib...");
            ImGui::TextDisabled("Requires ERI.dylib alongside ERIImGuiOverlay.dylib.");
            ImGui::TextDisabled("Path: Payload/App.app/Frameworks/ERI.dylib");''')

m = once(m,
    '    ImGui::End(); ImGui::PopStyleColor(7); ImGui::PopStyleVar(4);',
    '''    if(diagnostic && *diagnostic && tabs && count) {
        ImGui::TextDisabled("Bridge: %s", diagnostic);
    }
    ImGui::End(); ImGui::PopStyleColor(7); ImGui::PopStyleVar(4);''')
overlay.write_text(s)
menu_cpp.write_text(m)
menu_h.write_text(h)
print("ERI runtime bridge diagnostics and retry patch applied")
