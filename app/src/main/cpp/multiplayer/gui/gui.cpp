#include "../main.h"
#include "../game/game.h"
#include "../net/netgame.h"
#include "gui.h"
#include "../playertags.h"
#include "../net/playerbubblepool.h"
#include "vendor/str_obfuscator/str_obfuscator.hpp"
#include "../game/Mobile/MobileMenu/MobileMenu.h"
// voice
#include "../voice_new/Plugin.h"
#include "../voice_new/MicroIcon.h"
#include "../voice_new/SpeakerList.h"
#include "../voice_new/Network.h"

#include "../gui/samp_widgets/voicebutton.h"
#include "game/Textures/TextureDatabaseRuntime.h"
#include "game/Streaming.h"
#include "game/Pools.h"
#include "game/CGPS.hpp"

#include "../java/jniutil.h"

extern GPS* pGPS;
extern CNetGame* pNetGame;
extern CPlayerTags* pPlayerTags;
extern CJavaWrapper* pJavaWrapper;
extern UI* pUI;
extern CMobileMenu* CMobileMenu;
extern CSettings* pSettings;
extern CGame* pGame;

static bool bAFKInitialized = false;

UI::UI(const ImVec2& display_size, const std::string& font_path)
        : Widget(), ImGuiWrapper(display_size, font_path), m_pJavaWrapper(nullptr)
{
    UISettings::Initialize(display_size);
    this->setFixedSize(display_size);

    // Загружаем сохраненное состояние FPS info
    if(pSettings) {
        m_bShowFPSInfo = pSettings->GetFPSInfo();
    }
}

bool UI::initialize()
{
    if (!ImGuiWrapper::initialize()) return false;

    m_splashScreen = new SplashScreen();
    this->addChild(m_splashScreen);
    m_splashScreen->setFixedSize(size());
    m_splashScreen->setPosition(ImVec2(0.0f, 0.0f));
    m_splashScreen->setVisible(true);

    m_chat = new Chat();
    this->addChild(m_chat);
    m_chat->setFixedSize(UISettings::chatSize());
    m_chat->setPosition(UISettings::chatPos());
    m_chat->setItemSize(UISettings::chatItemSize());
    m_chat->setVisible(false);

    m_buttonPanel = new ButtonPanel();
    this->addChild(m_buttonPanel);
    m_buttonPanel->setFixedSize(UISettings::buttonPanelSize());
    m_buttonPanel->setPosition(UISettings::buttonPanelPos());
    m_buttonPanel->setVisible(false);

    m_voiceButton = new VoiceButton();
    this->addChild(m_voiceButton);
    m_voiceButton->setFixedSize(UISettings::buttonVoiceSize());
    m_voiceButton->setPosition(UISettings::buttonVoicePos());
    m_voiceButton->setVisible(false);

    m_spawn = new Spawn();
    this->addChild(m_spawn);
    m_spawn->setFixedSize(UISettings::spawnSize());
    m_spawn->setPosition(UISettings::spawnPos());
    m_spawn->setVisible(false);

    m_dialog = new Dialog();
    this->addChild(m_dialog);
    m_dialog->setVisible(false);
    m_dialog->setMinSize(UISettings::dialogMinSize());
    m_dialog->setMaxSize(UISettings::dialogMaxSize());

    m_keyboard = new Keyboard();
    this->addChild(m_keyboard);
    m_keyboard->setFixedSize(UISettings::keyboardSize());
    m_keyboard->setPosition(UISettings::keyboardPos());
    m_keyboard->setVisible(false);

    m_playerTabList = new PlayerTabList();
    //this->addChild(m_playerTabList);

    label = new Label(" ", ImColor(1.0f, 1.0f, 1.0f), true, UISettings::fontSize() / 2);
    pUI->addChild(label);

    label2 = new Label(" ", ImColor(1.0f, 1.0f, 1.0f), true, UISettings::fontSize() / 2);
    pUI->addChild(label2);

    label3 = new Label(" ", ImColor(1.0f, 1.0f, 1.0f), true, UISettings::fontSize() / 2);
    pUI->addChild(label3);

    label4 = new Label(" ", ImColor(1.0f, 1.0f, 1.0f), true, UISettings::fontSize() / 2);
    pUI->addChild(label4);

/*
    Label* d_label1;
    if(VER_x32)
    {
        d_label1 = new Label(cryptor::create("2.10 SA-MP (armeabi-v7a)").decrypt(), ImColor(1.0f, 1.0f, 1.0f), true, UISettings::fontSize() / 2.5);
        this->addChild(d_label1);
        d_label1->setPosition(ImVec2(3.0, 3.0));
    }
    else
    {
        d_label1 = new Label(cryptor::create("2.10 SA-MP (arm64-v8a)").decrypt(), ImColor(1.0f, 1.0f, 1.0f), true, UISettings::fontSize() / 2.5);
        this->addChild(d_label1);
        d_label1->setPosition(ImVec2(3.0, 3.0));
    }
*/

    return true;
}

#include "../game/CGPS.hpp"
void UI::render()
{
    ImGuiWrapper::render();

    if(m_bShowFPSInfo) {
        renderDebug();
    }
    ShowSpeed();

    ProcessPushedTextdraws();

    if (m_bNeedClearMousePos) {
        ImGuiIO& io = ImGui::GetIO();
        io.MousePos = ImVec2(-1, -1);
        m_bNeedClearMousePos = false;
    }

    if (m_playerTabList && m_playerTabList->visible())
        m_playerTabList->Tick();
}

void UI::ToggleFPSInfo()
{
    m_bShowFPSInfo = !m_bShowFPSInfo;

    // Сохраняем настройку
    if(pSettings) {
        pSettings->SetFPSInfo(m_bShowFPSInfo);
    }
}

void UI::shutdown()
{
    ImGuiWrapper::shutdown();
}

void UI::drawList()
{
    if (!visible()) return;

    if (pPlayerTags) pPlayerTags->Render(renderer());
    if (pNetGame && pNetGame->GetTextLabelPool()) pNetGame->GetTextLabelPool()->Render(renderer());
    if (pNetGame && pNetGame->GetPlayerBubblePool()) pNetGame->GetPlayerBubblePool()->Render(renderer());

    if (pGPS) pGPS->DoPathDraw();

    draw(renderer());
}

void UI::touchEvent(const ImVec2& pos, TouchType type)
{
    if(!visible()) return;

    if (m_keyboard->visible() && m_keyboard->contains(pos))
    {
        m_keyboard->touchEvent(pos, type);
        return;
    }

    if (m_dialog->visible() && m_dialog->contains(pos))
    {
        m_dialog->touchEvent(pos, type);
        return;
    }

    Widget::touchEvent(pos, type);
}

enum eTouchType
{
    TOUCH_POP = 1,
    TOUCH_PUSH = 2,
    TOUCH_MOVE = 3
};

bool UI::OnTouchEvent(int type, bool multi, int x, int y)
{
    ImGuiIO& io = ImGui::GetIO();

    VoiceButton* vbutton = pUI->voicebutton();
    switch (type)
    {
        case TOUCH_PUSH:
            io.MousePos = ImVec2(x, y);
            io.MouseDown[0] = true;
            break;

        case TOUCH_POP:
            io.MouseDown[0] = false;
            m_bNeedClearMousePos = true;
            break;

        case TOUCH_MOVE:
            io.MousePos = ImVec2(x, y);
            break;
    }

    return true;
}

#include "../settings.h"
void UI::renderDebug()
{
    if(!pSettings->Get().iFPSCounter) return;

    if (m_chat->active() || m_keyboard->visible())
    {
        label->setText(" ");
        label2->setText(" ");
        label3->setText(" ");
        label4->setText(" ");
        return;
    }

    char szStr[30];
    char szStrMem[64];
    char szStrPos[64];

    ImVec2 pos = ImVec2(pUI->ScaleX(40.0f), pUI->ScaleY(1080.0f - UISettings::fontSize() * 3.5));

    static float fps = 120.f;
    static auto lastTick = CTimer::m_snTimeInMillisecondsNonClipped;
    if(CTimer::m_snTimeInMillisecondsNonClipped - lastTick > 500) {
        lastTick = CTimer::m_snTimeInMillisecondsNonClipped;
        fps = std::clamp(CTimer::game_FPS, 10.f, (float) 120);
    }
    if(VER_x32)
    {
        snprintf(&szStr[0], sizeof(szStr), "x32 FPS: %.0f", fps);
    }
    else
    {
        snprintf(&szStr[0], sizeof(szStr), "x64 FPS: %.0f", fps);
    }

    label->setText(&szStr[0]);
    label->setPosition(pos);

    auto &msUsed = CStreaming::ms_memoryUsed;
    auto &msAvailable = CStreaming::ms_memoryAvailable;

    struct mallinfo memInfo = mallinfo();
    int totalAllocatedMB  = memInfo.uordblks / (1024 * 1024);

    snprintf(&szStrMem[0], sizeof(szStrMem), "MEM: %d mb (stream %d/%d) (Tex %d MB)",
             totalAllocatedMB,
             msUsed / (1024 * 1024),
             msAvailable / (1024 * 1024),
             TextureDatabaseRuntime::storedTexels / (1024 * 1024)
    );

    if (totalAllocatedMB >= 600)
    {
        CStreaming::MakeSpaceFor(500);
    }

    pos = ImVec2(pUI->ScaleX(40.0f), pUI->ScaleY(1080.0f - UISettings::fontSize() * 3));

    label2->setText(&szStrMem[0]);
    label2->setPosition(pos);

    if (pGame && pGame->FindPlayerPed() && pGame->FindPlayerPed()->m_pPed)
    {
        snprintf(&szStrPos[0], sizeof(szStrPos), "POS: %.2f, %.2f, %.2f",
                 pGame->FindPlayerPed()->m_pPed->m_matrix->m_pos.x,
                 pGame->FindPlayerPed()->m_pPed->m_matrix->m_pos.y,
                 pGame->FindPlayerPed()->m_pPed->m_matrix->m_pos.z);
        pos = ImVec2(pUI->ScaleX(40.0f), pUI->ScaleY(1080.0f - UISettings::fontSize() * 2.5));
        label3->setText(&szStrPos[0]);
        label3->setPosition(pos);
    }

    char debugPools[250];
    snprintf(&debugPools[0], sizeof(debugPools), "NSingle: %d/100000; NDouble: %d/60000; Peds: %d/240; Veh's: %d/1000; Obj: %d/3000; EntryInf: %d/60000; Dummies: %d/80000, Buildings: %d/60000",
             GetPtrNodeSingleLinkPool()->GetNoOfUsedSpaces(),
             GetPtrNodeDoubleLinkPool()->GetNoOfUsedSpaces(),
             GetPedPoolGta()->GetNoOfUsedSpaces(),
             GetVehiclePoolGta()->GetNoOfUsedSpaces(),
             GetObjectPoolGta()->GetNoOfUsedSpaces(),
             GetEntryInfoNodePool()->GetNoOfUsedSpaces(),
             GetDummyPool()->GetNoOfUsedSpaces(),
             GetBuildingPool()->GetNoOfUsedSpaces()
    );

    pos = ImVec2(pUI->ScaleX(40.0f), pUI->ScaleY(1080.0f - UISettings::fontSize() * 0.5));
    label4->setText(&debugPools[0]);
    label4->setPosition(pos);
}

void UI::PushToBufferedQueueTextDrawPressed(uint16_t textdrawId)
{
    BUFFERED_COMMAND_TEXTDRAW* pCmd = m_BufferedCommandTextdraws.WriteLock();

    pCmd->textdrawId = textdrawId;

    m_BufferedCommandTextdraws.WriteUnlock();
}

void UI::ProcessPushedTextdraws()
{
    BUFFERED_COMMAND_TEXTDRAW* pCmd = nullptr;
    while (pCmd = m_BufferedCommandTextdraws.ReadLock())
    {
        RakNet::BitStream bs;
        bs.Write(pCmd->textdrawId);
        pNetGame->GetRakClient()->RPC(&RPC_ClickTextDraw, &bs, HIGH_PRIORITY, RELIABLE_SEQUENCED, 0, false, UNASSIGNED_NETWORK_ID, 0);
        m_BufferedCommandTextdraws.ReadUnlock();
    }
}

void UI::SetEat(float eate){
    eat = static_cast<int>(eate);
}
void UI::SetDrink(float drinke){
    drink = static_cast<int>(drinke);
}

void UI::SetBankMoney(uint32_t bank)
{
    bankMoney = bank;
    FLog("SetBankMoney: %u", bankMoney);
}

int UI::GetEat(){
    return eat;
}

int UI::GetDrink(){
    return drink;
}
int UI::GetBankMoney() {
    return bankMoney;
}

void UI::ShowSpeed(){
    if (!pJavaWrapper) return;

    if (!pGame || !pNetGame || !pGame->FindPlayerPed()->IsInVehicle()) {
        pJavaWrapper->HideSpeed();
        return;
    }
    if (pGame->FindPlayerPed()->IsAPassenger()) {
        pJavaWrapper->HideSpeed();
        return;
    }
    //скрытие спидака ебучего #CRMPGOVNO
    if (gMobileMenu->pendingScreen){
        pJavaWrapper->HideSpeed();
        return;
    }

    int i_speed = 0;
    float fHealth = 0;
    CVehicle *pVehicle = nullptr;
    CVehiclePool *pVehiclePool = pNetGame->GetVehiclePool();
    CPlayerPed *pPlayerPed = pGame->FindPlayerPed();
    VEHICLEID id = pVehiclePool->FindIDFromGtaPtr(pPlayerPed->GetGtaVehicle());
    pVehicle = pVehiclePool->GetAt(id);

    if (pUI->m_chat->active()){
        pJavaWrapper->HideSpeed();
        return;
    }

    if(pPlayerPed)
    {
        if(pVehicle)
        {
            CVector vecMoveSpeed = pVehicle->m_pVehicle->GetMoveSpeed();;
            i_speed = sqrt((vecMoveSpeed.x * vecMoveSpeed.x) + (vecMoveSpeed.y * vecMoveSpeed.y) + (vecMoveSpeed.z * vecMoveSpeed.z)) * 180;
            bHealth = pVehicle->GetHealth()/10;
        }
    }
    pJavaWrapper->ShowSpeed();
    pJavaWrapper->UpdateSpeedInfo(i_speed, m_fuel, bHealth, bMeliage, bEngine, bLights, 0, bDoor);
}