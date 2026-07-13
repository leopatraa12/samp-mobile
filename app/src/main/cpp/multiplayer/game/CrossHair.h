//
// Created by x1y2z on 10.11.2023.
//

#ifndef BKUZN_CROSSHAIR_H
#define BKUZN_CROSSHAIR_H


#include "RW/rwcore.h"
#include "sprite2d.h"

class CCrossHair {
public:
    static inline CSprite2d* pCircleTex;
    static inline bool m_UsedCrossHair{};

public:
    static void Init();
    static void Render();
};


#endif //BKUZN_CROSSHAIR_H
