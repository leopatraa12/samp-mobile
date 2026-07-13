//
// Created by x1y2z on 20.09.2023.
//

#ifndef BKUZN_TIMECYCLE_H
#define BKUZN_TIMECYCLE_H


#include "ColourSet.h"

class CTimeCycle {

public:
    static inline CColourSet m_CurrentColours;
    static inline RwRGBA m_BelowHorizonGrey;

public:
    static void InjectHooks();
};


#endif //BKUZN_TIMECYCLE_H
