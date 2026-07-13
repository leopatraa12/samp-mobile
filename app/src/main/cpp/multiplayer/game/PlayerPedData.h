#ifndef BKUZN_PLAYERPEDDATA_H
#define BKUZN_PLAYERPEDDATA_H

#include "common.h"

class CEntityGTA;

class CPlayerPedData {
public:
    void*   m_pWanted;
    void*   m_pPedClothesDesc;
    void*   m_pArrestingCop;

    CVector2D m_vecFightMovement;

    float   m_fMoveBlendRatio;
    float   m_fTimeCanRun;
    float   m_fMoveSpeed;

    uint8   m_nChosenWeapon;
    uint8   m_nCarDangerCounter;

    uint32  m_nStandStillTimer;
    uint32  m_nHitAnimDelayTimer;

    float   m_fAttackButtonCounter;   // 🔥 IMPORTANTE PRO TIRO

    void*   m_pDangerCar;

    uint32  m_nPlayerFlags; // 🔥 não fragmentar isso

    uint32  m_nPlayerGroup;
    uint32  m_nAdrenalineEndTime;

    uint8   m_nDrunkenness;
    uint8   m_nFadeDrunkenness;
    uint8   m_nDrugLevel;
    uint8   m_nScriptLimitToGangSize;

    float   m_fBreath;

    uint32  m_nMeleeWeaponAnimReferenced;
    uint32  m_nMeleeWeaponAnimReferencedExtra;

    float   m_fFPSMoveHeading;
    float   m_fLookPitch;

    uint32  m_nLastTimeFiring;   // 🔥 CRÍTICO

    uint32  m_nTargetBone;
    CVector m_vecTargetBoneOffset;

    bool    m_bPlayerSprintDisabled;
    bool    m_bDontAllowWeaponChange;

    uint16  m_nPadDownPressedInMilliseconds;
    uint16  m_nPadUpPressedInMilliseconds;

} __attribute__((packed));

VALIDATE_SIZE(CPlayerPedData, sizeof(CPlayerPedData));

#endif