# MiniShopApi

一個前後端分離的購物網站後端專案，涵蓋會員系統、商品瀏覽（含換頁與關鍵字查詢）、購物車、訂單流程，以及銷售報表 PDF 匯出。資料存取層以 Dao 介面統一對外，每個模組底下同時準備了 JPA 與 MyBatis 兩種實作，透過 `@Qualifier` 切換要注入哪一版，藉此練習依賴反轉（Dependency Inversion）在真實專案中的樣子。

## 技術棧

| 分類 | 技術 |
| --- | --- |
| 語言 / 框架 | Java 21、Spring Boot 4.1.1 |
| Web | Spring Web MVC |
| 安全性 | Spring Security（Session-based 登入，非 JWT） |
| 資料存取 | Spring Data JPA、MyBatis 3.0.4（每個模組都有兩版實作，見下方架構說明） |
| 資料庫 | MySQL（本機開發），並已預備 PostgreSQL 驅動供雲端部署使用 |
| 報表 | JasperReports 7.0.1（含中文 PDF 匯出） |
| 版本控制 | Git |
| 其他 | Lombok、Spring Boot DevTools |

## 系統架構

```
Controller → Service → Dao（介面） → DaoImpl（MyBatis 版 / JPA 版，二選一注入）
                                        ├─ XxxMyBatisImpl → Mapper + Mapper.xml
                                        └─ XxxJpaImpl     → Repository（Spring Data JPA）
```

五個模組（Member、Product、CartItem、Order、OrderItem）的 Dao 介面底下，都同時存在 `XxxMyBatisImpl` 與 `XxxJpaImpl` 兩個實作類別，各自標註不同的 Bean 名稱（例如 `productDaoMyBatis` / `productDaoJpa`）。因為同一個介面有兩個候選 Bean，Spring 在注入時無法自動判斷要用哪一個，所以每個 Service 建構子上都用 `@Qualifier("xxxDaoMyBatis")` 明確指定——目前全部模組預設都指定 MyBatis 版，未來要切換成 JPA 版，只要把對應 `@Qualifier` 裡的字串改掉即可，Service／Controller 完全不用改動，這正是 Dao 介面抽象化底層技術的用意。

| 模組 | 目前使用 | 也有實作 |
| --- | --- | --- |
| Member（會員） | MyBatis | JPA |
| Product（商品） | MyBatis | JPA |
| CartItem（購物車） | MyBatis | JPA |
| Order（訂單） | MyBatis | JPA |
| OrderItem（訂單明細） | MyBatis | JPA |

## 功能模組

- **會員系統**：註冊、登入／登出（Session）、角色權限（MEMBER／ADMIN）、管理員可查詢會員列表（換頁＋帳號／姓名關鍵字查詢）並管理角色、刪除會員
- **商品瀏覽**：換頁（`page`／`size`）＋ 關鍵字查詢（商品名稱模糊搜尋），兩者可同時使用；單一商品查詢
- **購物車**：加入商品、修改數量、刪除品項（皆會先驗證品項是否屬於目前登入者本人，避免 IDOR）、結帳（結帳會建立訂單並清空購物車）
- **訂單**：查詢自己的訂單列表、查詢訂單明細（回傳含商品名稱的 `OrderItemDetail` DTO，並附擁有者權限檢查）
- **銷售報表**：以 JSON 或 PDF 匯出商品銷售彙總（銷售數量、營收），限管理員存取；PDF 內含正體中文，並自訂樣式（表頭配色、框線、對齊）

## API 端點

| 方法 | 路徑 | 說明 | 權限 |
| --- | --- | --- | --- |
| POST | `/api/members/register` | 註冊會員 | 開放 |
| POST | `/api/members/login` | 登入 | 開放 |
| POST | `/api/members/logout` | 登出 | 需登入 |
| GET | `/api/members/me` | 取得目前登入會員 | 需登入 |
| GET | `/api/members?page=&size=&keyword=` | 會員列表（換頁＋查詢，回傳 `PageResponse`，keyword 比對帳號／姓名） | ADMIN |
| PUT | `/api/members/{id}/role` | 修改會員角色 | ADMIN |
| DELETE | `/api/members/{id}` | 刪除會員 | ADMIN |
| GET | `/api/products?page=&size=&keyword=` | 商品列表（換頁＋查詢，回傳 `PageResponse`） | 開放 |
| GET | `/api/products/{id}` | 商品詳情 | 開放 |
| GET | `/api/cart` | 查詢購物車 | 需登入 |
| POST | `/api/cart` | 加入購物車 | 需登入 |
| PUT | `/api/cart/{id}` | 修改購物車品項數量 | 需登入 |
| DELETE | `/api/cart/{id}` | 刪除購物車品項 | 需登入 |
| POST | `/api/cart/checkout` | 結帳（建立訂單） | 需登入 |
| GET | `/api/orders` | 我的訂單列表 | 需登入 |
| GET | `/api/orders/{id}/items` | 訂單明細（回傳 `OrderItemDetail`，含商品名稱） | 需登入（限本人） |
| GET | `/api/reports/products` | 商品銷售彙總（JSON） | ADMIN |
| GET | `/api/reports/products/pdf` | 商品銷售彙總（PDF） | ADMIN |

`GET /api/products` 的回傳格式：

```json
{
  "content": [ { "id": 1, "name": "無線滑鼠", "price": 399, "stock": 50 } ],
  "page": 1,
  "size": 10,
  "totalElements": 30,
  "totalPages": 3
}
```

`keyword` 可不帶或帶空字串，代表不篩選、回傳全部商品的分頁結果。

## 資料庫設計

主要資料表對應的 Entity：`Member`、`Product`、`CartItem`、`Order`、`OrderItem`。`Order`／`OrderItem` 的關聯欄位使用外鍵 ID（`memberId`／`orderId`／`productId`）直接存取，取代 JPA 的 `@ManyToOne` 物件關聯，避免 N+1 查詢與序列化時的循環參照問題；`CartItem` 則保留了真正的 JPA 物件關聯（`@ManyToOne Member` / `@ManyToOne Product`），兩種寫法在專案裡並存，也是為了同時保留兩種資料存取技術的實際範例。

## 技術重點與踩坑紀錄

**1. Spring Data JPA 啟動時的全域驗證**
只要專案裡存在繼承 `JpaRepository` 的介面，ApplicationContext 啟動時就會掃描並驗證它所有的衍生查詢方法名稱，即使該介面完全沒有被注入使用也一樣。曾經因為 `Order`／`OrderItem` 的關聯欄位改名，導致舊的 Repository 介面裡的衍生查詢對不上新欄位名稱，直接讓應用程式啟動失敗（`PropertyReferenceException`）。這也是後續替每個模組同時保留 JPA／MyBatis 兩版實作時要特別注意的地方：就算只想用 MyBatis 版，JPA 版的 Repository 介面也一樣會在啟動時被驗證。

**2. 多實作 + `@Qualifier` 缺一不可**
一個介面有兩個實作 Bean 時，`NoUniqueBeanDefinitionException` 只會在真正被注入的地方出現。實際踩過的坑是：只替 `ProductServiceImpl` 加了 `@Qualifier`，卻漏掉同樣注入 `ProductDao` 的 `CartServiceImpl`，導致其中一個地方啟動失敗——因此改成兩版實作時，必須先用專案搜尋確認「這個介面所有的注入點」，逐一補上 `@Qualifier`，不能只改看得到的那一處。

**3. JasperReports 中文 PDF 字型雙軌設定**
字型設定分成兩個獨立角色：`fontName`（畫面排版量測用，需為系統實際安裝的字型家族名稱，例如 `PingFang TC`）與 `pdfFontName` / `pdfEncoding` / `pdfEmbedded`（PDF 匯出實際渲染用，需指向真實字型檔路徑並設定 `Identity-H` 編碼）。兩者搞混會在「畫面量測階段」與「PDF 匯出階段」分別丟出不同例外。另外也實際遇過 GUI 編輯工具（Jaspersoft Studio）在儲存時用記憶體中的舊內容蓋掉外部剛修改過的檔案，說明了版本控制在這類「檔案即設定」情境下的必要性。

**4. MyBatis 換頁：手動 SQL vs. JPA 內建 Pageable**
MyBatis 版換頁要自己拼 `LIMIT`/`OFFSET`、手動用 `(page - 1) * size` 算 offset，並另外寫一支 `COUNT(*)` 查詢算總筆數；JPA 版則是 `productRepository.findAll(PageRequest.of(page - 1, size))` 就內建處理好分頁與計數（`PageRequest` 是 0-based，所以要減 1）。查詢功能也是一樣的落差：MyBatis 要在 XML 用 `<if>` 動態組 `WHERE ... LIKE`，JPA 則是宣告 `findByNameContaining(keyword, pageable)` 這種衍生查詢方法名稱就自動生成 SQL。這組對照很適合用來說明「全自動 ORM」與「半自動 ORM」實際開發體感上的差異。

**5. DevTools 不會自動同步 `src/main/resources` 的外部修改**
Spring Boot DevTools 監看的是 `target/classes`，如果直接在 IDE 外部（例如用腳本）修改 `src/main/resources` 底下的檔案（`.jrxml`、Mapper `.xml`），必須手動同步一份到對應的 `target/classes` 路徑，或是讓 IDE 重新建置，否則應用程式重啟後讀到的還是舊內容。

**6. 購物車 IDOR（Insecure Direct Object Reference）弱點修復**
原本修改／刪除購物車品項是直接用 `cartItemId` 查到資料就執行，沒有驗證這筆資料是不是「目前登入者本人」的購物車，等於任何登入的使用者只要猜得到別人的 `cartItemId`，就能改動或刪除別人購物車裡的東西。修法一開始想用 `cartItemDao.findById(id)` 查出來後比對 `item.getMember().getId()`，但先檢查 `CartItemMapper.xml` 才發現 MyBatis 版的 `resultMap` 根本沒有把 `member` 這個關聯查出來（只查了 `product`），這樣比對永遠會是 `null`。改用 `cartItemDao.findByMember(member)` 先撈出「本人所有」的購物車列表，再用 stream 比對目標 id 是否在裡面，同時避開了關聯欄位未映射的問題，也讓修法同時對 MyBatis／JPA 兩種實作都成立。

**7. `OrderItem` 只存外鍵 ID，前端不能直接拿到商品名稱**
`OrderItem`／`Order` 為了避免 N+1 查詢與序列化循環參照（見上方「資料庫設計」），關聯欄位設計成單純的外鍵 `Long`（`productId`），而不是 JPA 物件關聯。這代表 `/api/orders/{id}/items` 原本回傳的 `OrderItem` 物件裡沒有商品名稱，只有一個數字 ID。解法是新增一個 `OrderItemDetail` DTO，在 Service 層逐筆用 `productId` 查一次商品名稱組成新物件再回傳（商品已被刪除的情況會顯示「（商品已下架）」而不是 null），把「資料庫關聯怎麼設計」與「API 該回傳什麼形狀」這兩個決策分開處理。

## 部署準備

目前資料庫連線、CORS 允許來源、Session Cookie 的 `SameSite`／`Secure` 設定，都已經改成用環境變數帶入（見 `application.properties`），本機開發沒有設定這些環境變數時會自動 fallback 成本機 MySQL、`http://localhost:5173` 等預設值，行為與改動前一致。規劃中的部署方式：

- 後端：Render（免費方案）
- 資料庫：Render 內建的免費 PostgreSQL（`pom.xml` 已加入 PostgreSQL 驅動）
- 前端：Vercel

部署時需要在 Render 設定的環境變數：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`DB_DRIVER`（填 `org.postgresql.Driver`）、`CORS_ALLOWED_ORIGIN`（填實際前端網址）、`COOKIE_SAME_SITE=none`、`COOKIE_SECURE=true`（跨網域的 Session Cookie 必須設定，否則登入後續請求會被瀏覽器擋掉）。

Render 免費方案的資料庫閒置一段時間會被回收，重新建立後需要重新填入連線資訊；商品資料與測試用管理員帳號（`admin`）都會在應用程式啟動時自動重新建立（見 `DataInitializer`），額外手動註冊的會員資料則不會自動還原。

## 快速開始

1. 建立 MySQL 資料庫 `mini_shop`
2. 依實際環境調整資料庫帳密（可直接改 `application.properties` 預設值，或改用環境變數 `DB_URL`／`DB_USERNAME`／`DB_PASSWORD`）
3. 執行 `mvn spring-boot:run`（或在 IDE 中啟動 `MiniShopApiApplication`）
4. 首次啟動會自動建立測試商品資料與一組測試管理員帳號：`admin` / `admin123`

## 後續規劃

- 補齊單元測試
- `.jrxml` 內字型檔路徑目前為本機絕對路徑，尚未處理跨環境部署的可攜性
- 清理專案中殘留但未使用的 JPA 標註
- 實際完成雲端部署（Render + Postgres + Vercel），並驗證跨網域 Session Cookie 在不同瀏覽器（尤其 Safari／Firefox）下的行為
- 將登入機制由 Session 改為 JWT（Access Token + Refresh Token 雙 token），搭配 Redis 儲存 Refresh Token，處理登出撤銷與無狀態驗證
