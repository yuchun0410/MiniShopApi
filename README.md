# MiniShopApi

一個前後端分離的購物網站後端專案，涵蓋會員系統、商品瀏覽（含換頁與關鍵字查詢）、商品上架與附件上傳、購物車、訂單流程，以及銷售報表 PDF 匯出。資料存取層以 Dao 介面統一對外，每個模組底下同時準備了 JPA 與 MyBatis 兩種實作，透過 `@Qualifier` 切換要注入哪一版，藉此練習依賴反轉（Dependency Inversion）在真實專案中的樣子。

## 技術棧

| 分類 | 技術 |
| --- | --- |
| 語言 / 框架 | Java 21、Spring Boot 4.1.1 |
| Web | Spring Web MVC |
| 安全性 | Spring Security + JWT（Access Token + Refresh Token 雙 token，RSA 非對稱式簽章，STATELESS，不使用 Session） |
| 快取 / Token 儲存 | Redis（儲存 Refresh Token，登出即刪除；未連 Redis 的環境可切回記憶體版實作） |
| 資料存取 | Spring Data JPA、MyBatis 3.0.4（每個模組都有兩版實作，見下方架構說明） |
| 資料庫 | MySQL（本機開發），並已預備 PostgreSQL 驅動供雲端部署使用 |
| 報表 | JasperReports 7.0.1（含中文 PDF 匯出） |
| 版本控制 | Git |
| 其他 | Lombok、Spring Boot DevTools |

## 系統架構

```
Client → JwtAuthenticationFilter（解析 Authorization: Bearer token，設定 memberId）
       → Controller → Service → Dao（介面） → DaoImpl（MyBatis 版 / JPA 版，二選一注入）
                                                 ├─ XxxMyBatisImpl → Mapper + Mapper.xml
                                                 └─ XxxJpaImpl     → Repository（Spring Data JPA）
```

六個模組（Member、Product、ProductAttachment、CartItem、Order、OrderItem）的 Dao 介面底下，都同時存在 `XxxMyBatisImpl` 與 `XxxJpaImpl` 兩個實作類別，各自標註不同的 Bean 名稱（例如 `productDaoMyBatis` / `productDaoJpa`）。因為同一個介面有兩個候選 Bean，Spring 在注入時無法自動判斷要用哪一個，所以每個 Service 建構子上都用 `@Qualifier("xxxDaoMyBatis")` 明確指定——目前全部模組預設都指定 MyBatis 版，未來要切換成 JPA 版，只要把對應 `@Qualifier` 裡的字串改掉即可，Service／Controller 完全不用改動，這正是 Dao 介面抽象化底層技術的用意。

| 模組 | 目前使用 | 也有實作 |
| --- | --- | --- |
| Member（會員） | MyBatis | JPA |
| Product（商品） | MyBatis | JPA |
| ProductAttachment（商品附件） | MyBatis | JPA |
| CartItem（購物車） | MyBatis | JPA |
| Order（訂單） | MyBatis | JPA |
| OrderItem（訂單明細） | MyBatis | JPA |

## 功能模組

- **會員系統**：註冊、登入／登出、角色權限（MEMBER／ADMIN）、管理員可查詢會員列表（換頁＋帳號／姓名關鍵字查詢）並管理角色、刪除會員
- **JWT 驗證**：登入同時簽發 Access Token（15 分鐘）與 Refresh Token（7 天），皆採 RSA 非對稱式簽章（private key 簽發、public key 驗證）；Refresh Token 存進 Redis，登出時刪除；`/api/members/refresh` 用 Refresh Token 換發新的 Access Token
- **商品瀏覽**：換頁（`page`／`size`）＋ 關鍵字查詢（商品名稱模糊搜尋），兩者可同時使用；單一商品查詢
- **商品上架／附件上傳**：管理員可上傳檔案（格式不限，PDF／Excel／圖片皆可，也可不附檔案）同時建立新商品，檔案內容直接存進資料庫（BLOB），`@Transactional` 包住「新增商品」與「新增附件紀錄」兩筆資料庫寫入；管理員也可幫既有商品更換附件（不用刪掉重建整個商品），或刪除商品（連同附件紀錄一起刪除）
- **商品附件預覽**：公開端點依附件實際的 content-type 動態決定回應格式，圖片可直接用 `<img>` 顯示縮圖，其他格式（PDF、Excel）提供下載／開啟連結；商品名稱設有唯一鍵限制，重複上架同名商品會收到明確的錯誤訊息
- **購物車**：加入商品、修改數量、刪除品項（皆會先驗證品項是否屬於目前登入者本人，避免 IDOR）、結帳（結帳會建立訂單並清空購物車）
- **訂單**：查詢自己的訂單列表、查詢訂單明細（回傳含商品名稱的 `OrderItemDetail` DTO，並附擁有者權限檢查）
- **銷售報表**：以 JSON 或 PDF 匯出商品銷售彙總（銷售數量、營收），限管理員存取；PDF 內含正體中文，並自訂樣式（表頭配色、框線、對齊）

## API 端點

| 方法 | 路徑 | 說明 | 權限 |
| --- | --- | --- | --- |
| POST | `/api/members/register` | 註冊會員 | 開放 |
| POST | `/api/members/login` | 登入（回傳 member + accessToken + refreshToken） | 開放 |
| POST | `/api/members/refresh` | 用 Refresh Token 換發新的 Access Token | 需帶 Refresh Token |
| POST | `/api/members/logout` | 登出（刪除 Redis 中的 Refresh Token） | 需登入 |
| GET | `/api/members/me` | 取得目前登入會員 | 需登入 |
| GET | `/api/members?page=&size=&keyword=` | 會員列表（換頁＋查詢，回傳 `PageResponse`，keyword 比對帳號／姓名） | ADMIN |
| PUT | `/api/members/{id}/role` | 修改會員角色 | ADMIN |
| DELETE | `/api/members/{id}` | 刪除會員 | ADMIN |
| GET | `/api/products?page=&size=&keyword=` | 商品列表（換頁＋查詢，回傳 `PageResponse`） | 開放 |
| GET | `/api/products/{id}` | 商品詳情 | 開放 |
| POST | `/api/products` | 上架新商品＋上傳附件（multipart/form-data，附件選填） | ADMIN |
| GET | `/api/products/{id}/attachment` | 讀取商品附件實際內容，依 content-type 決定回應格式，供 `<img>`／`<iframe>` 直接顯示 | 開放 |
| POST | `/api/products/{id}/attachment` | 幫既有商品更換附件（一個商品只留一筆，會覆蓋舊的） | ADMIN |
| DELETE | `/api/products/{id}` | 刪除商品（含附件紀錄） | ADMIN |
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

主要資料表對應的 Entity：`Member`、`Product`、`ProductAttachment`、`CartItem`、`Order`、`OrderItem`。`Order`／`OrderItem` 的關聯欄位使用外鍵 ID（`memberId`／`orderId`／`productId`）直接存取，取代 JPA 的 `@ManyToOne` 物件關聯，避免 N+1 查詢與序列化時的循環參照問題；`CartItem` 則保留了真正的 JPA 物件關聯（`@ManyToOne Member` / `@ManyToOne Product`），兩種寫法在專案裡並存，也是為了同時保留兩種資料存取技術的實際範例。`ProductAttachment` 同樣只存 `productId` 這個純外鍵欄位（沒有用 `@ManyToOne`），代表商品刪除時不會被資料庫外鍵擋下來，需要在程式碼裡自行處理附件紀錄的清除；檔案內容本身存放在 `file_data`（`LONGBLOB`）欄位，明確指定 `columnDefinition`、不依賴 Hibernate 自動判斷型別。`Product.name` 設有唯一鍵（`unique = true`），避免同名商品重複上架。

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
Spring Boot DevTools 監看的是 `target/classes`，如果直接在 IDE 外部（例如用腳本）修改 `src/main/resources` 底下的檔案（`.jrxml`、Mapper `.xml`、`application.properties`），必須手動同步一份到對應的 `target/classes` 路徑，或是在 IDE 裡按重新整理（Eclipse 是 F5）讓 IDE 重新建置，否則應用程式重啟後讀到的還是舊內容。

**6. 購物車 IDOR（Insecure Direct Object Reference）弱點修復**
原本修改／刪除購物車品項是直接用 `cartItemId` 查到資料就執行，沒有驗證這筆資料是不是「目前登入者本人」的購物車，等於任何登入的使用者只要猜得到別人的 `cartItemId`，就能改動或刪除別人購物車裡的東西。修法一開始想用 `cartItemDao.findById(id)` 查出來後比對 `item.getMember().getId()`，但先檢查 `CartItemMapper.xml` 才發現 MyBatis 版的 `resultMap` 根本沒有把 `member` 這個關聯查出侅（只查了 `product`)，這格比對永遠是新同時除容兛虽。改用 `cartItemDao.findByMember(member)` 先撈出「本人所有」的購物車列表，再用 stream 比對目標 id 是否在裡面，同時避開了關聯欄位未映射的問題，也讓修法同時對 MyBatis／JPA 兩種實作都成立。

**7. `OrderItem` 只存外鍵 ID，前端不能直接拿到商品名稱**
`OrderItem`／`Order` 為了避免 N+1 查詢與序列化循環參照（見上方「資料庫設計」），關聯欄位設計成單純的外鍵 `Long`（`productId`），而不是 JPA 物件關聯。這代表 `/api/orders/{id}/items` 原本回傳的 `OrderItem` 物件裡沒有商品名稱，只有一個數字 ID。解法是新增一個 `OrderItemDetail` DTO，在 Service 層逐筆用 `productId` 查一次商品名稱組成新物件再回傳（商品已被刪除的情況會顯示「（商品已下架）」而不是 null），把「資料庫關聯怎麼設計」與「API 該回傳什麼形狀」這兩個決策分開處理。

**8. RSA 私鑰的 PKCS1 / PKCS8 格式陷阱**
`openssl genpkey -algorithm RSA` 產生的 PEM 檔本身就是 PKCS8 格式（`-----BEGIN PRIVATE KEY-----`），但如果再用 `openssl pkey -in xxx.pem -outform DER` 重新轉一次 DER，OpenSSL 3.x 會把它「解包」回最原始的 PKCS1 `RSAPrivateKey` 格式（純數字欄位的 SEQUENCE），而不是 Java `PKCS8EncodedKeySpec` 需要的 PKCS8 `PrivateKeyInfo` 包裝格式（多包了演算法識別資訊），載入時會丟出 `InvalidKeySpecException: algid parse error, not a sequence`。用 `openssl asn1parse` 比對兩者的 ASN.1 結構才確認問題所在。修法是直接從原始 PEM 檔裡把 Base64 內容取出來（只去掉頭尾的 `-----BEGIN/END PRIVATE KEY-----`），不要再經過 `openssl pkey -outform DER` 這道轉換。

**9. `@Transactional` 不保護檔案系統寫入，以及後來把檔案整個搬進資料庫的原因**
商品附件功能原本設計成「檔案先寫進硬碟，再進行資料庫交易」：如果交易中途失敗，Spring 會自動 rollback 資料庫的部分，但硬碟上已經寫入的檔案不會自動消失，因此在 `catch` 區塊裡手動呼叫刪除檔案的補償邏輯，避免留下沒有對應資料庫紀錄的孤兒檔案；刪除商品則是反過來，先確定資料庫那步成功，才刪除硬碟上的實體檔案。這組設計具體示範了 `@Transactional` 的實際保護範圍：只涵蓋 JDBC／資料庫操作，不涵蓋任何檔案系統或外部服務的呼叫。

後來把這個設計整個換成把檔案內容直接存進資料庫（`product_attachment.file_data`，`LONGBLOB`），原因是本機硬碟在部署到雲端（例如 Render 免費方案）之後，應用程式一重啟本機檔案系統就會被清空，之前寫進硬碟的附件會憑空消失。換成 BLOB 之後，商品與附件這兩筆資料庫寫入變成唯一需要處理的動作，兩者包在同一個 `@Transactional` 裡就已經足夠，不再需要上述那套手動補償刪除的邏輯——因為檔案內容本身現在也是交易的一部分，rollback 會自動把它一起復原。這也是一個實際權衡：BLOB 換來部署時的資料一致性，代價是資料庫備份/還原的體積變大、無法直接用 CDN 前置附件，資料量成長到一定程度後通常還是要改回物件儲存服務（例如 S3），只是現階段優先解決「重啟就消失」這個更急迫的問題。

**10. Spring 的 CORS 檢查對「實際請求」一樣會比對 `allowedMethods`**
前端偵測商品是否有附件時，會先送一個 `HEAD` 請求探測 content-type，卻一直收到失敗、被 `.catch()` 吃掉當成「沒有附件」處理。追查後發現 `SecurityConfig` 的 `CorsConfigurationSource` 裡 `setAllowedMethods` 沒有列出 `HEAD`——Spring 的 `DefaultCorsProcessor` 不只檢查 preflight（`OPTIONS`）請求，連「實際請求」本身的 method 也會拿去跟 `allowedMethods` 比對，即使 `HEAD` 本身是瀏覽器安全清單內的方法，只要沒列在允許清單裡一樣會被擋下來，而且擋下來的回應不帶 CORS header，瀏覽器只會回報成一次失敗的請求，不會有明確的錯誤訊息，排查起來比較花時間。

**11. 舊資料缺少新欄位時的向下相容備援**
附件的 `content_type` 欄位是後來才加上去的，在這之前上傳的舊資料這個欄位是 `null`，讀取時直接回傳會退回泛用的 `application/octet-stream`，導致瀏覽器一律當成不明檔案強制下載，即使實際上是圖片或 PDF 也無法直接預覽。解法是在讀取端點加一層備援：`content_type` 是 `null` 或空字串時，改用 `URLConnection.guessContentTypeFromName()` 依副檔名猜一次，猜不出來才真的退回 `application/octet-stream`。

**12. `ddl-auto=update` 的兩個實際限制**
這個專案親身踩過 `ddl-auto=update` 兩種不同方向的限制：一是只會新增，不會刪除或修改——BLOB 遷移把 `stored_file_name`／`file_path` 兩個欄位從 Entity 移除後，資料庫裡的舊欄位跟它們的 `NOT NULL` 限制不會自動消失，需要手動下 `ALTER TABLE ... DROP COLUMN`，不然新的 INSERT 會直接因為缺值違反舊限制而失敗；二是對「既有欄位事後追加約束」不可靠——幫 `Product.name` 加上 `unique = true` 之後，重啟應用程式並不保證會真的在資料庫建出對應的 UNIQUE 索引，同樣需要手動確認、必要時自己補一行 `ALTER TABLE ... ADD UNIQUE`。正式環境改用 Flyway 之類的 migration 工具、以明確的 SQL 腳本管理每一次結構變更，會是比較穩妥的做法。

**13. 同一個例外類型被多種情境共用，訊息語意衝突**
`GlobalExceptionHandler` 原本只有一支 `@ExceptionHandler(DataIntegrityViolationException.class)`，處理「商品還在購物車裡，刪除被外鍵擋下來」這個情境，訊息寫死成「此筆資料仍被其他資料參照，無法刪除」。幫 `Product.name` 加上唯一鍵之後才發現，唯一鍵重複時 MySQL／Spring 丟出的是 `DuplicateKeyException`——它其實是 `DataIntegrityViolationException` 的子類別，會被同一支 handler 接住，導致「新增商品名稱重複」這種完全不同的情境，也顯示「無法刪除」這種文不對題的訊息。解法是另外新增一支專門接 `DuplicateKeyException` 的 handler：Spring 解析 `@ExceptionHandler` 時，同一個 `@RestControllerAdvice` 裡若有多支候選 handler，會挑「繼承關係上最接近」實際丟出例外類型的那一支，因此重複鍵值會優先命中新的、語意正確的那支，外鍵限制擋下刪除則維持走原本那支。

## 部署準備

目前資料庫連線、CORS 允許來源，都已經改成用環境變數帶入（見 `application.properties`），本機開發沒有設定這些環境變數時會自動 fallback 成本機 MySQL、`http://localhost:5173` 等預設值，行為與改動前一致。規劃中的部署方式：

- 後端：Render（免費方案）
- 資料庫：Render 目前只有原生 PostgreSQL（無原生 MySQL），計畫改用 PostgreSQL 驅動或改接外部免費 MySQL（例如 Aiven）
- Redis：Render 的 Key Value 服務（Redis 協定相容），免費方案無持久化，服務重啟會清空 Refresh Token
- 前端：Vercel

部署時需要在 Render 設定的環境變數：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`DB_DRIVER`、`CORS_ALLOWED_ORIGIN`（填實際前端網址）、`JWT_PRIVATE_KEY`／`JWT_PUBLIC_KEY`（正式環境務必重新產生一組，不能沿用本機開發用的預設值）、`REDIS_HOST`／`REDIS_PORT`。商品附件內容已改存進資料庫（BLOB），不再依賴網頁服務本機硬碟，Render 免費方案硬碟非持久化、服務重啟或重新部署會清空本機檔案系統這個問題，對附件功能已經不受影響；仍要留意的是 MySQL 的 `max_allowed_packet` 設定，上傳較大的檔案時可能會在資料庫這層被擋下來，需要視實際部署環境調整。

## 快速開始

1. 建立 MySQL 資料庫 `mini_shop`，並確認本機有安裝並啟動 Redis（`redis-cli ping` 應回傳 `PONG`）
2. 依實際環境調整資料庫帳密（可直接改 `application.properties` 預設值，或改用環境變數 `DB_URL`／`DB_USERNAME`／`DB_PASSWORD`）
3. 執行 `mvn spring-boot:run`（或在 IDE 中啟動 `MiniShopApiApplication`）
4. 首次啟動會自動建立測試商品資料與一組測試管理員帳號：`admin` / `admin123`

## 後續規劃

- 補齊自動化測試（尤其是 `@Transactional` rollback 情境，適合用 Mockito 驗證交易失敗時的補償邏輯是否有正確執行）
- `.jrxml` 內字型檔路徑目前為本機絕對路徑，尚未處理跨環境部署的可攜性
- 清理專案中殘留但未使用的 JPA 標註
- 實際完成雲端部署（Render + 資料庫 + Redis + Vercel），並驗證正式環境下 JWT 驗證與檔案上傳功能是否正常運作
- 評估 BLOB 儲存附件的長期取捨：目前選擇 BLOB 是為了優先解決「網頁服務重啟本機檔案就消失」這個立即的部署痛點，等資料量或流量成長到一定程度，可能需要重新評估改接物件儲存服務（例如 S3）
- 正式環境的資料庫結構變更改用 Flyway 之類的 migration 工具管理，取代目前依賴 `ddl-auto=update` 的做法（見踩坑紀錄第 12 點）
