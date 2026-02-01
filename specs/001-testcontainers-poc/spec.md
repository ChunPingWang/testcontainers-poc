# Feature Specification: Testcontainers Integration Testing PoC

**Feature Branch**: `001-testcontainers-poc`
**Created**: 2026-02-01
**Status**: Draft
**Input**: PRD.md - Testcontainers Integration Testing PoC for Enterprise Financial Systems

## Clarifications

### Session 2026-02-01

- Q: 測試容器在同一測試類別內的多個測試方法間應如何管理？ → A: 同一測試類別共享容器（類別間隔離，效率較佳）
- Q: 當整合測試失敗時，系統應自動收集哪些診斷資訊？ → A: 完整診斷包（容器日誌、網路狀態、資源使用量）
- Q: CI 環境中 Testcontainers 應採用哪種 Docker 存取策略？ → A: 兩者皆支援（DinD 與 socket 掛載），依 CI 平台自動選擇
- Q: CI 環境應如何管理容器映像？ → A: 使用內部 registry 快取（穩定性佳，避免外部 registry 限流）

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 本機執行單一場景測試 (Priority: P1)

作為開發人員，我希望能在本機執行單一測試場景（如訂單處理流程），只啟動該場景所需的容器，不需要等待所有容器啟動，也不需要連接共享的測試環境。

**Why this priority**: 這是 PoC 的核心價值驗證，證明 Testcontainers 能解決環境依賴問題，讓開發人員快速獲得測試反饋。

**Independent Test**: 可透過執行單一模組的測試指令驗證，確認僅啟動該模組宣告的容器，測試通過後容器自動清理。

**Acceptance Scenarios**:

1. **Given** 開發人員已安裝 Docker，**When** 執行單一模組測試指令，**Then** 僅啟動該模組所需的容器，測試完成後容器自動清理
2. **Given** 本機無任何測試容器運行，**When** 執行基礎整合場景測試，**Then** 資料庫與訊息佇列容器自動啟動，測試執行完畢後停止
3. **Given** 多個測試同時執行，**When** 各測試使用獨立的容器實例，**Then** 測試之間無資料汙染或互相干擾

---

### User Story 2 - 訂單處理端對端測試 (Priority: P1)

作為 QA 工程師，我希望能驗證訂單從建立、儲存、發佈事件到狀態更新的完整流程，確保系統各元件整合正確。

**Why this priority**: 端對端流程驗證是整合測試的核心目標，直接影響軟體品質信心。

**Independent Test**: 可透過呼叫訂單建立 API，驗證資料庫寫入、事件發佈、狀態更新的完整流程。

**Acceptance Scenarios**:

1. **Given** 系統已啟動測試容器，**When** 透過 API 建立訂單，**Then** 訂單成功儲存至資料庫並回傳成功狀態
2. **Given** 訂單已成功儲存，**When** 系統發佈訂單建立事件，**Then** 消費者接收事件並更新訂單狀態為已確認
3. **Given** 訂單狀態已更新，**When** 查詢訂單，**Then** 回傳最新的訂單狀態

---

### User Story 3 - 多儲存層資料一致性驗證 (Priority: P1)

作為開發人員，我希望能驗證主資料庫、快取、搜尋索引三層儲存之間的資料一致性，確保客戶查詢在任何儲存層都能取得正確資料。

**Why this priority**: 多儲存層架構是金融系統常見模式，資料一致性直接影響業務正確性。

**Independent Test**: 可透過寫入主資料庫，驗證快取更新、索引同步、快取失效後回退機制。

**Acceptance Scenarios**:

1. **Given** 資料寫入主資料庫，**When** 快取更新機制觸發，**Then** 快取在合理時間內反映最新資料
2. **Given** 快取已過期，**When** 查詢資料，**Then** 系統自動從主資料庫取得資料並更新快取
3. **Given** 主資料庫資料變更，**When** 索引同步機制觸發，**Then** 搜尋索引在合理時間內反映最新資料
4. **Given** 搜尋索引已同步，**When** 執行全文搜尋，**Then** 搜尋結果與主資料庫資料一致

---

### User Story 4 - 資料庫 Schema 遷移測試 (Priority: P2)

作為資料庫管理者，我希望能驗證 Schema 遷移在乾淨的資料庫上可正確執行，確保每次部署的變更不會造成問題。

**Why this priority**: Schema 遷移錯誤會導致系統無法啟動或資料損壞，需要在隔離環境中驗證。

**Independent Test**: 可透過啟動乾淨的資料庫容器，執行遷移腳本，驗證 Schema 版本與結構正確。

**Acceptance Scenarios**:

1. **Given** 啟動乾淨的資料庫容器，**When** 執行 Schema 遷移，**Then** 所有遷移腳本按順序執行成功
2. **Given** 遷移執行過程中發生錯誤，**When** 測試執行，**Then** 測試立即失敗並顯示明確錯誤訊息
3. **Given** 遷移執行完成，**When** 檢查 Schema 版本，**Then** 版本與預期一致

---

### User Story 5 - CI 模組化平行建置 (Priority: P2)

作為 DevOps 工程師，我希望 CI 能依據模組變更範圍觸發對應的測試，不會因為不相關模組的變更而重新測試。

**Why this priority**: 平行建置能大幅縮短 CI 時間，提升團隊開發效率。

**Independent Test**: 可透過變更特定模組的程式碼，驗證僅觸發該模組的 CI 流程。

**Acceptance Scenarios**:

1. **Given** 變更特定場景模組的程式碼，**When** 推送至版本控制，**Then** 僅觸發該模組的測試流程
2. **Given** 變更共用模組的程式碼，**When** 推送至版本控制，**Then** 觸發所有依賴該共用模組的測試流程
3. **Given** 多個模組同時變更，**When** CI 執行，**Then** 各模組測試可平行執行

---

### User Story 6 - 事件串流與 Schema 演進測試 (Priority: P2)

作為架構師，我希望能驗證事件串流在 Schema 版本演進時仍能正確運作，確保新舊版本消費者的相容性。

**Why this priority**: Schema 演進是事件驅動架構的關鍵挑戰，相容性問題會導致服務中斷。

**Independent Test**: 可透過發送新版本 Schema 的事件，驗證舊版本消費者能正確處理。

**Acceptance Scenarios**:

1. **Given** 生產者使用新版本 Schema 發送事件，**When** 舊版本消費者接收事件，**Then** 事件能被正確解析
2. **Given** 嘗試註冊不相容的 Schema 變更，**When** Schema 註冊中心驗證，**Then** 拒絕該變更並回傳錯誤
3. **Given** 同一分區鍵的事件，**When** 多筆事件發送，**Then** 事件按順序被消費

---

### User Story 7 - 資料變更捕獲測試 (Priority: P2)

作為資料工程師，我希望能驗證資料庫變更能被即時捕獲並發佈為事件，建立可靠的資料同步管道。

**Why this priority**: CDC 是建立即時資料管道的關鍵技術，需要驗證其可靠性。

**Independent Test**: 可透過對資料庫執行新增、修改、刪除操作，驗證對應的變更事件被發佈。

**Acceptance Scenarios**:

1. **Given** 資料庫有新增、修改、刪除操作，**When** CDC 機制捕獲變更，**Then** 對應的變更事件在合理時間內被發佈
2. **Given** 變更事件被發佈，**When** 檢查事件內容，**Then** 包含變更前後的完整資料快照
3. **Given** CDC 連接器重新啟動，**When** 恢復捕獲，**Then** 從上次位置繼續，無遺失事件

---

### User Story 8 - 外部系統故障韌性測試 (Priority: P2)

作為開發人員，我希望能驗證系統在外部服務故障時能正確降級，不會全面崩潰。

**Why this priority**: 韌性是金融系統的關鍵要求，需要在可控環境中驗證降級機制。

**Independent Test**: 可透過模擬外部服務故障，驗證熔斷器、重試、回退機制的行為。

**Acceptance Scenarios**:

1. **Given** 外部服務回傳錯誤，**When** 系統處理請求，**Then** 使用預設的回退回應
2. **Given** 外部服務延遲過長，**When** 請求逾時，**Then** 正確觸發逾時處理
3. **Given** 連續多次請求失敗，**When** 達到熔斷閾值，**Then** 熔斷器開啟，後續請求直接走回退路徑
4. **Given** 熔斷器處於半開啟狀態，**When** 請求成功，**Then** 熔斷器回復關閉狀態

---

### User Story 9 - 身份驗證與授權測試 (Priority: P3)

作為資安工程師，我希望能驗證身份驗證與角色授權流程在隔離環境中正確運作。

**Why this priority**: 安全測試需要在隔離環境中執行，避免影響正式環境的安全設定。

**Independent Test**: 可透過模擬登入流程，驗證令牌簽發、角色授權、令牌更新機制。

**Acceptance Scenarios**:

1. **Given** 使用正確帳號密碼，**When** 登入驗證服務，**Then** 成功取得存取令牌
2. **Given** 持有管理員角色令牌，**When** 存取管理端點，**Then** 請求成功
3. **Given** 持有一般使用者角色令牌，**When** 存取管理端點，**Then** 請求被拒絕
4. **Given** 持有更新令牌，**When** 存取令牌過期後更新，**Then** 成功取得新的存取令牌

---

### User Story 10 - 動態憑證管理測試 (Priority: P3)

作為 DevOps 工程師，我希望能驗證應用程式能動態取得資料庫憑證，不需要硬編碼密碼。

**Why this priority**: 動態憑證是現代安全最佳實踐，需要驗證與憑證管理服務的整合。

**Independent Test**: 可透過啟動應用程式，驗證能從憑證管理服務取得資料庫連線憑證。

**Acceptance Scenarios**:

1. **Given** 應用程式啟動，**When** 需要資料庫連線，**Then** 從憑證管理服務取得臨時憑證
2. **Given** 憑證即將過期，**When** 憑證更新機制觸發，**Then** 自動更新憑證不中斷服務

---

### User Story 11 - 雲端服務離線測試 (Priority: P3)

作為開發人員，我希望能在本機測試雲端服務整合，不需要真實的雲端帳號。

**Why this priority**: 脫離雲端帳號測試能降低成本並加速開發迭代。

**Independent Test**: 可透過使用模擬的雲端服務，驗證檔案儲存、訊息佇列等操作。

**Acceptance Scenarios**:

1. **Given** 使用模擬的物件儲存服務，**When** 上傳、下載、刪除檔案，**Then** 操作成功且行為與真實服務一致
2. **Given** 使用模擬的訊息佇列服務，**When** 發送、接收訊息，**Then** 操作成功包含死信佇列處理
3. **Given** 使用模擬的文件資料庫服務，**When** 執行 CRUD 操作，**Then** 操作成功

---

### User Story 12 - 微服務契約測試 (Priority: P3)

作為架構師，我希望能在部署前自動驗證微服務之間的 API 契約，及早發現破壞性變更。

**Why this priority**: 契約測試能防止微服務整合問題進入生產環境。

**Independent Test**: 可透過消費者產生契約、提供者驗證契約，確保 API 相容性。

**Acceptance Scenarios**:

1. **Given** 消費者定義預期的 API 契約，**When** 契約產生，**Then** 契約被儲存至契約管理服務
2. **Given** 提供者實作變更，**When** 執行契約驗證，**Then** 驗證結果回報給契約管理服務
3. **Given** 部署前執行部署檢查，**When** 存在不相容的契約變更，**Then** 阻擋部署

---

### Edge Cases

- 容器啟動失敗時，測試應立即失敗並提供明確的錯誤訊息
- Docker 服務未啟動時，測試應優雅地失敗而非無限等待
- 本機記憶體不足時，應提供警告並建議只執行單一模組測試
- 網路問題導致容器映像無法下載時，應顯示明確的錯誤訊息
- 測試執行過程中容器意外停止，應能重新啟動或優雅失敗
- 多個測試同時執行時，埠號衝突應自動處理

## Requirements *(mandatory)*

### Functional Requirements

**Phase 1: 核心基礎**

- **FR-001**: 系統 MUST 提供獨立的測試模組，每個模組可單獨執行測試
- **FR-002**: 系統 MUST 在測試開始時自動啟動所需的容器
- **FR-003**: 系統 MUST 在測試類別結束後自動清理容器資源（同一測試類別內的測試方法共享容器實例）
- **FR-004**: 系統 MUST 提供共用模組供各場景模組重用容器定義與測試基底類別
- **FR-005**: 系統 MUST 支援資料庫 Schema 自動遷移
- **FR-006**: 系統 MUST 支援訊息佇列的事件發佈與消費測試
- **FR-007**: 系統 MUST 支援多儲存層（主資料庫、快取、搜尋索引）的整合測試
- **FR-008**: 系統 MUST 確保各測試之間資料隔離，無互相干擾
- **FR-008a**: 系統 MUST 在測試失敗時自動收集完整診斷資訊（容器日誌、網路狀態、資源使用量）

**Phase 2: 事件驅動與韌性**

- **FR-009**: 系統 MUST 支援事件串流平台的整合測試
- **FR-010**: 系統 MUST 支援 Schema 註冊與驗證
- **FR-011**: 系統 MUST 支援資料變更捕獲（CDC）的整合測試
- **FR-012**: 系統 MUST 支援外部服務模擬與故障注入
- **FR-013**: 系統 MUST 支援網路延遲、斷線等故障情境模擬
- **FR-014**: 系統 MUST 支援熔斷器、重試、回退機制的驗證

**Phase 3: 安全、雲端與契約**

- **FR-015**: 系統 MUST 支援身份驗證服務的整合測試
- **FR-016**: 系統 MUST 支援動態憑證管理服務的整合測試
- **FR-017**: 系統 MUST 支援雲端服務模擬（物件儲存、訊息佇列、文件資料庫）
- **FR-018**: 系統 MUST 支援消費者驅動契約測試流程
- **FR-019**: 系統 MUST 支援契約版本管理與部署檢查

### Key Entities

- **測試場景 (Test Scenario)**: 代表一個獨立的測試模組，包含所需的容器定義、測試案例、驗證邏輯
- **測試容器 (Test Container)**: 代表一個可啟動的容器定義，包含映像名稱、埠號對應、環境變數、啟動檢查
- **訂單 (Order)**: 代表業務實體，用於端對端測試驗證，包含識別碼、狀態、建立時間、客戶資訊
- **事件 (Event)**: 代表領域事件，用於事件驅動架構測試，包含事件類型、發生時間、內容
- **契約 (Contract)**: 代表消費者與提供者之間的 API 契約，包含請求格式、回應格式、版本

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 測試容器啟動成功率達到 99% 以上
- **SC-002**: 單一模組測試套件執行時間不超過 90 秒
- **SC-003**: 全部模組測試套件執行時間不超過 8 分鐘
- **SC-004**: 連續 10 次執行測試無隨機失敗（Flaky Test 發生率 0%）
- **SC-005**: 所有開發人員皆能在本機成功執行測試（100% 本機可執行率）
- **SC-006**: 每個模組可獨立建置與測試，模組間零耦合
- **SC-007**: 整合測試程式碼覆蓋率達到每模組 80% 以上
- **SC-008**: 契約測試覆蓋 90% 以上的 API 端點
- **SC-009**: 所有故障注入場景測試通過率 100%
- **SC-010**: 快取更新在資料寫入後 1 秒內完成
- **SC-011**: 搜尋索引同步在資料變更後 5 秒內完成
- **SC-012**: CDC 事件在資料庫操作後 3 秒內產生

## Assumptions

- 開發人員本機已安裝 Docker Engine 20.10 或以上版本
- 本機至少有 8GB RAM 可供執行完整測試套件
- 團隊使用 Java 21 與 Spring Boot 3.4.x 作為主要開發技術
- CI 環境支援 Docker-in-Docker 或 Docker socket 掛載（系統自動偵測並選擇適當策略）
- 容器映像可從公開容器註冊中心下載

## Out of Scope

- 效能壓力測試（Performance / Load Testing）
- 容器編排（Kubernetes / Docker Compose 部署）
- 正式環境部署方案
- UI / E2E 瀏覽器測試
- 多雲架構設計
- 合規審計報告產出
