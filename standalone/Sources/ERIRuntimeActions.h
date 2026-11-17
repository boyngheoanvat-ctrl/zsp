#pragma once
#include <cstdint>
#include <string>

// Experimental, explicitly invoked IL2CPP runtime actions.
// Requires an actual instance found in a strong-typed static manager reference.
// No unsafe raw-offset dereferences, fixed version offsets, or old dylib load.
std::string ERIV5RuntimeStatus();
bool ERIV5ProbeVehicle();
bool ERIV5PreviewGarageVehicle(uint32_t vehicleID);
bool ERIV5QuickMatch(int32_t mode, uint8_t playerCount, int32_t mapID, int32_t useMap);
bool ERIV5ReplayWaitingShadow();
