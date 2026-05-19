package com.ctbc.ebookstore.config;

import com.ctbc.ebookstore.bean.*;
import com.ctbc.ebookstore.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Profile("!test")
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private final AppUserRepository userRepo;
    private final CategoryRepository categoryRepo;
    private final BookRepository bookRepo;
    private final CartRepository cartRepo;
    private final WalletRepository walletRepo;
    private final TopUpCodeRepository topUpCodeRepo;
    private final PlatformWalletRepository platformWalletRepo;
    private final PublisherWalletRepository publisherWalletRepo;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(AppUserRepository userRepo,
                      CategoryRepository categoryRepo,
                      BookRepository bookRepo,
                      CartRepository cartRepo,
                      WalletRepository walletRepo,
                      TopUpCodeRepository topUpCodeRepo,
                      PlatformWalletRepository platformWalletRepo,
                      PublisherWalletRepository publisherWalletRepo,
                      PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.categoryRepo = categoryRepo;
        this.bookRepo = bookRepo;
        this.cartRepo = cartRepo;
        this.walletRepo = walletRepo;
        this.topUpCodeRepo = topUpCodeRepo;
        this.platformWalletRepo = platformWalletRepo;
        this.publisherWalletRepo = publisherWalletRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void run(String... args) {
        System.out.println("========== 初始化電子書商店資料 ==========");

        // ── 建立使用者 ──────────────────────────────
        AppUser admin  = createUserIfAbsent("admin1",   "admin1@test.com",   "password", "ADMIN");
        AppUser sellerA = createUserIfAbsent("seller1", "seller1@test.com",  "password", "SELLER"); // 星河出版社
        AppUser sellerB = createUserIfAbsent("seller2", "seller2@test.com",  "password", "SELLER"); // 遠見書局
        AppUser sellerC = createUserIfAbsent("seller3", "seller3@test.com",  "password", "SELLER"); // 知識殿堂
        AppUser user1  = createUserIfAbsent("user1",    "user1@test.com",    "password", "USER");
        AppUser user2  = createUserIfAbsent("user2",    "user2@test.com",    "password", "USER");

        // ── 建立 Cart ────────────────────────────────
        createCartIfAbsent(user1);
        createCartIfAbsent(user2);
        createCartIfAbsent(sellerA);
        createCartIfAbsent(sellerB);
        createCartIfAbsent(sellerC);
        createCartIfAbsent(admin);

        // ── 建立 User Wallet（使用者購書用） ──────────────
        createWalletIfAbsent(user1,   "user",   new BigDecimal("5000"));
        createWalletIfAbsent(user2,   "user",   new BigDecimal("3000"));
        // 出版商也保留購書錢包
        createWalletIfAbsent(sellerA, "seller", new BigDecimal("8000"));
        createWalletIfAbsent(sellerB, "seller", new BigDecimal("6000"));
        createWalletIfAbsent(sellerC, "seller", new BigDecimal("4500"));

        // ── 建立 Platform Wallet（全系統唯一，初始餘額 0） ───
        createPlatformWalletIfAbsent();

        // ── 建立 Publisher Wallet（出版商分潤錢包，初始 0） ──
        createPublisherWalletIfAbsent(sellerA);
        createPublisherWalletIfAbsent(sellerB);
        createPublisherWalletIfAbsent(sellerC);

        // ── 建立分類 ─────────────────────────────────
        Category scifi      = createCategoryIfAbsent("科幻",   "科幻小說");
        Category literature = createCategoryIfAbsent("文學",   "文學作品");
        Category story      = createCategoryIfAbsent("故事",   "故事小說");
        Category history    = createCategoryIfAbsent("歷史",   "歷史人文");
        Category selfhelp   = createCategoryIfAbsent("勵志",   "勵志成長");
        Category mystery    = createCategoryIfAbsent("推理",   "推理懸疑");

        // ── 建立書籍 ─────────────────────────────────
        if (bookRepo.count() == 0) {

            // seller1（星河出版社）— 主打科幻 & 文學
            createBook("三體",             "劉慈欣",    "科幻巨著，獲得雨果獎",           new BigDecimal("299"), scifi,      sellerA, "https://placehold.co/200x300?text=Three+Body");
            createBook("三體II：黑暗森林", "劉慈欣",    "宇宙文明的生存法則",             new BigDecimal("319"), scifi,      sellerA, "https://placehold.co/200x300?text=Dark+Forest");
            createBook("三體III：死神永生","劉慈欣",    "宇宙文明的終章",                 new BigDecimal("339"), scifi,      sellerA, "https://placehold.co/200x300?text=Death+End");
            createBook("活著",             "余華",      "人生如同活著本身",               new BigDecimal("199"), literature, sellerA, "https://placehold.co/200x300?text=Alive");
            createBook("許三觀賣血記",     "余華",      "小人物的生存掙扎",               new BigDecimal("189"), literature, sellerA, "https://placehold.co/200x300?text=Blood");
            createBook("百年孤寂",         "馬奎斯",    "魔幻寫實主義傑作",               new BigDecimal("279"), literature, sellerA, "https://placehold.co/200x300?text=Solitude");

            // seller2（遠見書局）— 主打歷史 & 勵志 & 故事
            createBook("人類簡史",         "尤瓦爾·哈拉瑞","解讀人類發展史",             new BigDecimal("349"), history,  sellerB, "https://placehold.co/200x300?text=Sapiens");
            createBook("未來簡史",         "尤瓦爾·哈拉瑞","數據主義的時代預言",         new BigDecimal("369"), history,  sellerB, "https://placehold.co/200x300?text=Homo+Deus");
            createBook("今日簡史",         "尤瓦爾·哈拉瑞","21世紀的21堂課",             new BigDecimal("329"), history,  sellerB, "https://placehold.co/200x300?text=21+Lessons");
            createBook("深夜食堂",         "安倍夜郎",  "日本風味小說",                   new BigDecimal("159"), story,    sellerB, "https://placehold.co/200x300?text=Midnight+Diner");
            createBook("蛤蟆先生去看心理醫生","羅伯·迪波德","療愈系兒童文學",            new BigDecimal("129"), selfhelp, sellerB, "https://placehold.co/200x300?text=Frog+Story");
            createBook("被討厭的勇氣",     "岸見一郎",  "阿德勒心理學的對話",             new BigDecimal("249"), selfhelp, sellerB, "https://placehold.co/200x300?text=Courage");

            // seller3（知識殿堂）— 主打推理 & 科幻 & 勵志
            createBook("解憂雜貨店",       "東野圭吾",  "溫暖人心的推理小說",             new BigDecimal("219"), mystery,  sellerC, "https://placehold.co/200x300?text=Naoko");
            createBook("白夜行",           "東野圭吾",  "黑暗中的愛與罪",                 new BigDecimal("259"), mystery,  sellerC, "https://placehold.co/200x300?text=White+Night");
            createBook("嫌疑犯X的獻身",   "東野圭吾",  "天才數學家的完美犯罪",           new BigDecimal("239"), mystery,  sellerC, "https://placehold.co/200x300?text=Devotion");
            createBook("星際大戰",        "喬治·盧卡斯", "原著小說改編",                  new BigDecimal("289"), scifi,    sellerC, "https://placehold.co/200x300?text=Star+Wars");
            createBook("原子習慣",         "詹姆斯·克利爾","微小改變帶來驚人成果",       new BigDecimal("279"), selfhelp, sellerC, "https://placehold.co/200x300?text=Atomic+Habits");
            createBook("刻意練習",         "安德斯·艾瑞克森","頂尖高手的精進法則",       new BigDecimal("299"), selfhelp, sellerC, "https://placehold.co/200x300?text=Peak");
        }

        // ── 建立儲值碼 ───────────────────────────────
        createTopUpCodeIfAbsent("CODE001", new BigDecimal("100"));
        createTopUpCodeIfAbsent("CODE002", new BigDecimal("500"));
        createTopUpCodeIfAbsent("CODE003", new BigDecimal("1000"));
        createTopUpCodeIfAbsent("VIP2024", new BigDecimal("2000"));
        createTopUpCodeIfAbsent("WELCOME", new BigDecimal("200"));

        populateBookContents();
        System.out.println("資料初始化完成");
        System.out.println("出版商帳號:");
        System.out.println("  seller1(星河出版社)/password — 三體系列、余華作品、百年孤寂");
        System.out.println("  seller2(遠見書局)/password   — 尤瓦爾系列、深夜食堂、被討厭的勇氣");
        System.out.println("  seller3(知識殿堂)/password   — 東野圭吾系列、原子習慣、刻意練習");
        System.out.println("使用者帳號: user1/password, user2/password");
        System.out.println("管理員帳號: admin1/password");
    }

    private AppUser createUserIfAbsent(String username, String email, String rawPassword, String role) {
        return userRepo.findByUsername(username).orElseGet(() -> {
            AppUser u = new AppUser(username, email, passwordEncoder.encode(rawPassword), role);
            return userRepo.save(u);
        });
    }

    private void createCartIfAbsent(AppUser user) {
        cartRepo.findByUser(user).orElseGet(() -> cartRepo.save(new Cart(user)));
    }

    private void createWalletIfAbsent(AppUser user, String type, BigDecimal initialBalance) {
        walletRepo.findByUser(user).orElseGet(() -> walletRepo.save(new Wallet(user, type, initialBalance)));
    }

    private Category createCategoryIfAbsent(String name, String description) {
        return categoryRepo.findAll().stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElseGet(() -> categoryRepo.save(new Category(name, description)));
    }

    private void createBook(String title, String author, String desc, BigDecimal price,
                            Category category, AppUser seller, String coverImage) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setDescription(desc);
        book.setPrice(price);
        book.setCategory(category);
        book.setSeller(seller);
        book.setCoverImage(coverImage);
        
        bookRepo.save(book);
    }

    private void populateBookContents() {
        java.util.Map<String, String> contents = new java.util.HashMap<>();

        contents.put("三體", """
第一章　靜默的春天

一九六七年，文化大革命進入了最瘋狂的階段。葉文潔站在人群中，看著父親被批鬥。台上的人揮舞著拳頭，台下的人跟著吼叫。她的心早已麻木，如同一塊沉入深海的石頭。

她父親葉哲泰曾是清華大學的物理學教授，一個相信科學的人。但此刻，科學無法保護他。

幾年後，葉文潔被送往內蒙古大興安嶺的林場勞動改造。在那片原始的森林裡，她第一次感受到了宇宙的靜默。夜晚，她仰望星空，心中湧起一個瘋狂的念頭——宇宙中還有其他智慧生命嗎？他們是否和人類一樣，充滿了自相殘殺的慾望？

第二章　紅岸基地

林場附近有一個神秘的軍事設施，周圍架設著巨大的天線。葉文潔被徵召進入這個代號「紅岸」的基地，從事無線電監聽工作。她漸漸意識到，這個基地的真正目的是向宇宙發射信號，尋找地外文明。

「我們應該保持靜默，」她的上司說，「宇宙是危險的。」

但葉文潔不這麼想。在那個年代，她已經對人類失去了信心。她秘密修改了發射參數，向宇宙深處發出了呼叫。

那個呼叫，改變了人類的命運。

第三章　三體世界

在距地球四光年的半人馬座三星系統中，一個文明正在掙扎求存。三顆恆星的引力相互撕扯，讓這個星球的氣候極度不穩定。有時連續數百年的「恆紀元」，有時又是毀滅性的「亂紀元」——太陽從天空中狂暴地運動，灼燒一切，或者突然消失，讓整個世界陷入冰封。

三體人已經文明了數千年，卻始終無法預測這種混沌的氣候。他們發展出了一種獨特的生存方式：在亂紀元來臨前，整個文明進入「脫水」狀態，等待危機過去後再「浸泡」復活。

但這一切都只是暫時的解決方案。三體文明需要一個穩定的家園。

當他們收到了來自地球的信號，一個大膽的計劃開始醞釀——入侵並佔領那顆擁有單一太陽的美麗藍色星球。
""");

        contents.put("三體II：黑暗森林", """
第一章　面壁者

聯合國大廈的會議室裡，地球領導人們面色凝重。三體艦隊正在以千分之一光速向地球逼近，四百年後將抵達。人類有四個世紀的時間準備，卻發現每一個計劃都在三體人的監視下暴露無遺。

三體人隨艦隊派出了「智子」——折疊在更高維度的質子，能夠即時監聽人類的一切通訊和會議。唯一無法被監視的，是人類的思想。

「面壁計劃」由此誕生。四名被選中的人，將在不透露任何信息的情況下制定各自的戰略。他們的真實意圖，永遠只存在於自己的腦海中。

第二章　羅輯的使命

羅輯是一個不修邊幅的社會學家，整日渾渾噩噩地過日子。他怎麼也想不到自己會被選為面壁者之一。

葉文潔在臨終前悄悄告訴了他一個宇宙秘密：宇宙社會學的兩個公理。

第一，生存是文明的第一需要。

第二，文明不斷增長和擴張，但宇宙中的物質總量保持不變。

從這兩個公理出發，可以推導出一個黑暗的結論——宇宙是一片黑暗森林，每個文明都是帶槍的獵人。任何暴露自己位置的文明，都將被消滅。這就是宇宙的生存法則，也是費米悖論的答案。

第三章　冬眠者的世界

為了等待未來的太空艦隊，大量人類進入冬眠狀態。羅輯也選擇了冬眠，他在這段漫長的睡眠中，緩緩思索著葉文潔留下的秘密。

當他醒來時，世界已經過去了兩個世紀。人類的文明在這兩百年間飛速發展，太空艦隊規模龐大，人們對未來充滿信心。但羅輯知道，那個黑暗森林法則的答案，即將改變一切。
""");

        contents.put("三體III：死神永生", """
第一章　程心的選擇

在人類文明即將迎來黑暗打擊的時代，一個叫程心的年輕女性工程師做出了一個改變歷史的選擇。她向自己仰慕的男友雲天明的大腦捐贈了所有積蓄，讓這顆大腦被送往三體艦隊，成為人類的探子。

幾十年後，程心被選為「執劍人」——掌握著人類最後的核威懾，即向宇宙廣播三體星系坐標的能力。這是一種以自我毀滅為代價的威懾。

但當危機真正來臨的那一刻，程心無法扣下那個扳機。

第二章　黑暗打擊

宇宙的黑暗法則降臨了。一個來自二向箔的攻擊，將整個太陽系的三維空間降維成二維。太陽系在瞬間被壓平，所有的文明、所有的生命，都在降維的過程中消失。

只有少數人類乘坐飛船逃離了太陽系，成為宇宙中的漂泊者。

第三章　終末之戰

在宇宙的某個角落，人類文明的最後餘燼仍在燃燒。宇宙本身也在加速膨脹，時空的結構正在被撕裂。文明的終結，或許只是時間問題。

但在黑暗與絕望中，仍有一束微弱的光：雲天明的大腦已成功潛入三體文明，並通過童話的方式向人類傳遞了秘密。這個秘密，也許能拯救最後的人類。
""");

        contents.put("活著", """
第一章　少爺的日子

我年輕的時候，家裡是有錢的，那時候光是我家的農田就有一百畝。我整日在城裡鬼混，賭錢、嫖女人，把老爹氣得半死。家裡請了個賬房先生，叫徐老頭，他整天板著臉，說我是敗家子。

我娶了一個好女人，叫家珍，是米行老板的女兒。她賢惠勤勞，從不抱怨，只是每次看我回來的樣子，眼神裡都是說不出的委屈。

後來，我把家裡的一百畝地全輸光了。老爹氣死了，娘也越來越糊塗，家珍挺著大肚子，跟著我搬進了茅草屋。

第二章　土改的年代

就在我以為日子要這樣一直窮下去的時候，土改來了。原來地主龍二的那些田，分了一部分給我。我高興得不知怎麼辦才好，卻又隱隱覺得不安。

後來抓壯丁，我被抓去打仗。那幾年，我在戰場上看盡了死亡。子彈從耳邊飛過，炮聲震得耳朵嗡嗡響，身邊的人一個個倒下去，我卻奇怪地活著。

第三章　回家

仗打完了，我活著回來了。家珍還在，女兒鳳霞長高了，又生了個兒子有慶，瘦得像根竹竿。日子苦，但我們一家人在一起。

後來鳳霞聾啞了，有慶給縣長夫人獻血給抽死了。我哭過，也怨過，但生活還得繼續。

活著，有時候只是為了活著本身。
""");

        contents.put("許三觀賣血記", """
第一章　第一次賣血

許三觀是個絲廠的工人，他聽說農村人靠賣血換錢，就也去試了試。在縣醫院的走廊，他遇到了根龍和阿方，兩個老賣血鬼。他們教他怎麼在賣血前喝大量的水，讓血稀釋，這樣抽起來不傷身子。

許三觀第一次賣血，拿到了三十五塊錢。他拿這筆錢做了一件大事：娶了林芬芳的女兒許玉蘭為妻。

第二章　一樂的事

許三觀和許玉蘭有了三個兒子：一樂、二樂、三樂。可是有一天，許三觀發現一樂根本不是自己的兒子，是許玉蘭年輕時跟另一個男人何小勇生的。

許三觀大鬧了一番，但最後還是留下了一樂。孩子有什麼錯呢。

第三章　賣血的歲月

此後幾十年，每當家裡遇到困難，許三觀就去賣血。為了給一樂治病、為了送孩子上城裡、為了度過饑荒。他的血，一次次救了這個家。

年老的許三觀想再去賣血，卻被醫院拒絕了——他的血太老，沒有人要。他在街上哭了起來，那是他這輩子第一次為了自己哭。
""");

        contents.put("百年孤寂", """
第一章　馬孔多的誕生

多年以後，面對行刑隊，奧雷里亞諾·布恩迪亞上校將會回想起父親帶他去見識冰塊的那個遙遠的下午。那時，馬孔多是一個二十戶人家的村落，屋子都是用泥巴和蘆葦搭成的，建在一條河邊。

霍塞·阿卡迪奧·布恩迪亞是這個家族的始祖。他殺死了普魯登西奧·阿吉拉爾，帶著妻子烏蘇拉和一群朋友，穿越了無數山嶺，在一片土地上建立了馬孔多。

第二章　吉普賽人的魔法

每年，一群吉普賽人都會帶著新奇的發明來到馬孔多。有一年，他們帶來了磁鐵，霍塞·阿卡迪奧用它在地上拖行，試圖找到黃金。另一年，他們帶來了放大鏡，霍塞·阿卡迪奧相信它可以用來作為戰爭武器。

領頭的吉普賽人梅爾基亞德斯似乎是不會死去的，他每次「死亡」後都會再次出現，帶著更奇妙的知識。他給霍塞·阿卡迪奧留下了一些羊皮紙，上面寫著神秘的文字，家族中沒有人能解讀。

第三章　孤獨的傳承

布恩迪亞家族的每一代人，都被一種神秘的孤獨所困擾。奧雷里亞諾上校打了三十二場起義戰爭，全部失敗。他坐在工作室裡，一遍遍熔掉自己做的小金魚，重新再做，永無止境。

這個家族的命運，早已被梅爾基亞德斯寫在那些羊皮紙上。只有最後一個人，才能讀懂那段預言——關於這個家族的命運，以及馬孔多的終結。
""");

        contents.put("人類簡史", """
第一章　認知革命

大約七萬年前，一件奇妙的事情發生了。智人的大腦結構突然發生了改變，讓我們能夠想像那些根本不存在的事物。這就是「認知革命」。

在這之前，所有的人類物種都只能用有限的符號溝通，說明眼前實際存在的事物。但智人卻能聊鬼神、談公司、說國家——這些都是想象中的現實，根本沒有實體。

這種能力帶來了驚人的優勢。一隻黑猩猩無法說服另一隻黑猩猩「如果你今天幫我抓虱子，死後我保證你能上天堂」，但人類可以。這種對「想象現實」的共同信仰，讓數以千萬計的陌生人能夠團結合作。

第二章　農業革命的騙局

大約一萬年前，農業革命開始了。人類從狩獵採集者，變成了農耕定居者。

表面上看，農業提供了更穩定的食物來源，讓人口爆炸式增長。但個人的生活卻大幅惡化。農民的飲食比採集者更單調，更依賴少數幾種主食，一旦農作物歉收便面臨飢荒。農民還必須長時間從事繁重的體力勞動，骨骼分析顯示，農業社會的人比採集者更多患有背痛和關節炎。

農業革命是歷史上最大的騙局——小麥、水稻等植物馴化了人類，而不是人類馴化了它們。

第三章　帝國的崛起

統一世界的進程，是通過帝國的擴張完成的。波斯帝國、羅馬帝國、蒙古帝國——這些龐大的政治實體，通過征服將不同的文明納入同一個管理體系。

帝國的暴力與壓迫固然存在，但它們也傳播了思想、藝術、宗教和貿易。每一次帝國的崩潰，都留下了深刻的文化遺產，成為下一個文明的基礎。
""");

        contents.put("未來簡史", """
第一章　新的議程

幾千年來，人類一直在與飢荒、瘟疫和戰爭搏鬥。這三大禍患是人類歷史的永恆主題，無數智者將其視為神的旨意或自然的秩序。

然而，進入二十一世紀，局面開始改變。飢荒、瘟疫和戰爭仍然存在，但它們不再是不可克服的命運，而是可以被技術和政策解決的挑戰。

更重要的是，人類正在面對全新的議程：不死、幸福，以及成為神明。

第二章　長生不死的追求

矽谷的科技大佬們正在砸入巨額資金，研究如何延長人類壽命。谷歌成立了卡利科公司，目標是「解決死亡」。彼得·蒂爾宣稱死亡是「有待解決的問題」。

從生物學角度看，老化是細胞損傷的積累，而這些損傷理論上是可以修復的。奈米機器人可能在未來巡邏我們的血液，清除廢物，修復受損的DNA。

如果真的實現了長生不死，社會將會怎樣？那些有錢人能買到長壽，窮人卻只能按自然規律死去，這將是人類歷史上最大的不平等。

第三章　數據主義的崛起

也許未來最革命性的轉變不是長生不死，而是一種新的世界觀——數據主義。

數據主義認為，宇宙由數據流組成，任何現象或實體的價值，在於對數據處理的貢獻。人類不過是一種數據處理算法，而電腦算法遲早將超越人類這種生化算法的能力。

到那時，人類可能不再是世界的主宰，而是成為全球數據流中的一個節點。
""");

        contents.put("今日簡史", """
第一章　幻滅

二〇一六年是令人幻滅的一年。英國投票脫歐，美國選出了唐納德·川普，整個自由主義秩序突然岌岌可危。人們開始質疑：我們一直相信的那些故事，還能指引我們的未來嗎？

過去兩個世紀，人類生活在三個宏大故事之中：法西斯的故事說精英應該領導，共產主義的故事說工人階級應該領導，自由主義的故事說每個個體都應該領導自己的生活。二十一世紀的今天，前兩個故事已經崩潰，而第三個故事正在遭受懷疑。

第二章　就業市場的崩潰

人工智慧即將衝擊就業市場，這已不是未來的預言，而是正在發生的現實。

自動化已經消滅了工廠中的大量工作崗位，卡車司機、收銀員、銀行出納員都面臨被取代的威脅。但更令人擔憂的是，這次的浪潮不僅影響體力勞動者，連腦力工作者也無法倖免。律師、醫生、財務分析師——人工智慧都在這些領域展示出超越人類的能力。

那麼，在一個機器人做得比人類更好的世界裡，人類的工作在哪裡？

第三章　平等與社群

面對這些挑戰，我們不能只依靠政府或科技公司。我們需要重新思考社群的意義。

在數字時代，每個人都可以連接到全球網絡，但真正的歸屬感，仍然需要線下的、面對面的社群。讓鄰里相互認識的街角小店，或許比任何社交媒體都更能維繫社會的凝聚力。
""");

        contents.put("深夜食堂", """
第一章　深夜才開始的故事

我的食堂在新宿的一條小巷子裡，菜單只有幾樣，豬肉湯、啤酒、清酒。但只要客人想吃什麼，我會盡量做。

我的工作時間是深夜十二點到早晨七點，也就是俗稱的深夜食堂。

來這裡的人，大多帶著說不出口的事。有些是剛下班的上班族，有些是酒喝多了出來走走的人，有些是不知道去哪的人。每個人點菜的方式都不一樣，但吃飯的時候，大家都會說一些平常不說的話。

第二章　紅香腸和小林

小林是個無業遊民，每天晚上都來，點的永遠是一根紅香腸和一杯啤酒。

他說他以前是個廚師，在法國學過料理，後來跟老板吵架辭職了，之後就再也沒工作過。他的紅香腸要切成章魚形狀，這是他媽媽小時候給他切的方式。

有一天，小林帶了個女人來，說是他前女友找到他了，問他願不願意跟她結婚。我給他們做了兩根章魚形狀的紅香腸。後來我再也沒見到他，聽說他去了大阪，重新開了一家小餐館。

第三章　媽媽的味噌湯

有個老先生，每次來都只點味噌湯。他說他太太去世了，太太最拿手的就是味噌湯，他現在每天晚上都很想喝。

我按照他說的食譜做，他喝了一口，說，差了一點，但很接近了。

他每個星期三都來，連續來了三年。最後一次，他說謝謝你，我明天就要去跟她會合了。我不知道那句話是什麼意思，但我知道，有些人來這裡，是為了最後再吃一次他們想吃的東西。
""");

        contents.put("蛤蟆先生去看心理醫生", """
第一章　蛤蟆先生的憂鬱

蛤蟆先生病了。

他躺在床上，不想動，也不想吃東西，連他最愛的車也提不起興趣。窗外的春天明媚而美好，鳥兒在唱歌，風吹過柳條，但這一切與他無關。

鼴鼠、河鼠和獾叔叔都很擔心他，商量之後，鼴鼠帶著一份名片來敲他的門：蒼鷺心理師。

「我不需要心理師，」蛤蟆說，「我只是累了。」

「但如果你不去，我們都會擔心你，」鼴鼠說。

蛤蟆先生出於對朋友的愧疚，勉強撥了電話。

第二章　第一次諮商

蒼鷺的辦公室很安靜，牆上掛著一些畫，桌上有幾本厚厚的書。蒼鷺本人頭髮花白，有一雙敏銳的眼睛，說話不多，但每一句都讓蛤蟆心裡不舒服。

「你今天來，是因為你想來，還是別人叫你來的？」蒼鷺問。

蛤蟆想了很久，說，是別人叫我來的。

「那你願意配合諮商嗎？」

「……我願意試試。」

蒼鷺說，心理諮商不是魔法，不能讓你立刻快樂，但它能幫你了解自己。他問蛤蟆，你現在的心情用1到10分來描述，幾分？

蛤蟆想了想，說，2分。

第三章　童年的影子

在接下來的幾次談話裡，蒼鷺帶著蛤蟆回溯過去。蛤蟆說起父親對他的嚴格要求，說起自己從小就是「問題蛤蟆」，說起被父親當眾羞辱的記憶。

蒼鷺問，當那些事發生的時候，你心裡在想什麼？

蛤蟆說，我以為是我的錯，覺得自己很壞。

蒼鷺說，孩子常常這樣想，因為承認父母有問題太可怕了，所以只能怪自己。

那一刻，蛤蟆先生哭了。
""");

        contents.put("被討厭的勇氣", """
第一章　哲學家與憤世嫉俗的青年

一個年輕人聽說城市邊有一位哲學家，主張「人可以改變」，認為人人都能幸福，便帶著滿腔憤怒去找他辯論。

「人怎麼可能改變？」年輕人說，「我從小就沒有受到過好的對待，自卑、懦弱已經是我的一部分，這不是過去造成的嗎？」

哲學家搖搖頭，說，阿德勒說，決定我們自身的，不是過去的經歷，而是我們賦予那些經歷的意義。

年輕人瞪大了眼睛，這個說法太荒謬了。

第二章　目的論與原因論

哲學家解釋，佛洛伊德的心理學是「原因論」，認為現在的我是過去原因的結果。而阿德勒的心理學是「目的論」，認為我們是為了達成現在的某個目的，才產生各種情緒和行為。

以憤怒為例，人並不是因為憤怒才大聲叫嚷，而是為了達到某個目的——比如讓對方順從——才發出憤怒。憤怒是手段，不是原因。

年輕人感到困惑，但又無法輕易反駁。

第三章　被討厭的勇氣

真正的自由，哲學家說，是被討厭的勇氣。

我們之所以活得如此辛苦，是因為我們太在乎別人的評價，將自己的人生課題與別人的人生課題混為一談。別人如何看待你，是別人的課題，不是你的。你能做的，只是活出最真實的自己。

這當然會被某些人討厭，但那是他們的自由，也是你的自由。
""");

        contents.put("解憂雜貨店", """
第一章　三個小偷

翔太、幸平和敦也三個人逃進了一家老舊的雜貨店。他們剛剛搶了一個老太太的包，正慌不擇路地躲著警察。

店裡空無一人，佈滿灰塵，顯然廢棄很久了。三個人打算在這裡過夜，天亮再走。

奇怪的事情發生了：天亮之前，從店門口的信箱投進來一封信。

信上寫著一個年輕女孩的煩惱：她的男友是個自行車手，受了重傷，可能再也無法比賽了。她愛他，但也有自己的夢想。她應該繼續支持他，還是離開？

第二章　建議的重量

三個人看著這封信，不知道該怎麼辦。但敦也說，反正閒著也是閒著，我們來回信吧。

他們寫了一封帶著玩笑意味的信投進去，沒想到第二天，那個女孩又投來了信，說她認真地考慮了他們的建議，然後問了更深的問題。

三個小偷就這樣，開始認真地扮演「建議者」。他們漸漸意識到，自己的每一句話，都可能真的影響一個人的人生決定。

第三章　時光的秘密

後來他們才發現，那家雜貨店並不普通——它能跨越時間，讓過去的人和現在的人互相通信。

那些煩惱各不相同的來信者，有的在三十年前，有的在現在，有的在未來。但他們的問題，都出奇地相似：面對人生的岔路，應該怎麼選？
""");

        contents.put("白夜行", """
第一章　一九七七年冬

一九七七年冬天，大阪西成區的廢棄大樓裡，發現了一具男屍。死者是當地典當行的老板桐原洋介，死亡方式是他殺。

調查這個案件的刑警笹垣潤三走訪了附近的居民。在其中一棟公寓裡，他遇到了死者的兒子桐原亮司，和附近花街女子的女兒西本雪穗。兩個孩子當時年紀都很小，大約十歲左右。

笹垣沒有找到足夠的證據，案件陷入僵局，最終成了懸案。

第二章　二十年的影子

二十年後，笹垣退休，但他從未放棄那個案件。

他調查的過程中發現，在這二十年裡，桐原亮司和西本雪穗從未公開出現在同一個場合，但只要雪穗的身邊出現了問題，亮司就會悄悄出現解決它。就像影子一樣，跟著她，卻從不現身。

雪穗成了上流社會的女性，亮司則在灰色地帶游走，兩個人的命運，糾纏在那個寒冬的夜晚裡，再也沒有解開。

第三章　白夜的意義

書名來自雪穗說的一句話：「我的天空裡沒有太陽，總是黑夜，但並不暗，因為有東西代替了太陽。雖然沒有太陽那麼明亮，但對我來說已經足夠。憑藉著這份光，我便能把夜晚當成白天。」

那份光，是亮司。
""");

        contents.put("嫌疑犯X的獻身", """
第一章　完美的犯罪

三月二十一日，帝都大學數學系助教石神哲哉在路上遇到了鄰居靖子和女兒美里。靖子是個離婚的女人，獨自撫養女兒，開了一家便當店。

當天晚上，靖子的前夫富樫慎二突然上門，對她們拳打腳踢，索要金錢。在混亂中，靖子和美里失手將他殺死。

石神在隔壁聽到動靜，敲門進來，看到了這一切。

「把這件事交給我，」石神說，「我來處理。」

第二章　數學家的推理

石神是個天才數學家，在大學裡鬱鬱不得志，甚至曾想過自殺。但靖子的出現，成了他活下去的理由。

他設計了一個天衣無縫的不在場證明，轉移了警察的注意力，讓一個無辜的流浪漢的死亡成為「意外」，掩蓋了真正的案發時間和地點。

探偵伽利略湯川學開始介入案件，他是石神在大學時代的老朋友。當他看穿了石神的計謀，卻發現謎題遠比他想像的更深——因為石神的犯罪，根本不是為了掩蓋那個夜晚的真相。

第三章　愛的方程式

石神最終的告白，震驚了所有人。他所做的一切，不是為了保護靖子逃脫法律，而是一個更宏大、更悲哀的計劃——獻身，用他的生命，換取她的自由。

有些愛，從不需要被對方知道。
""");

        contents.put("星際大戰", """
第一章　帝國的陰影

很久以前，在遙遠的銀河系……

銀河帝國以鐵腕統治著無數星球，恐懼是他們最強大的武器。黑暗原力的統治者達斯·維德親自乘坐死星，那是一個能夠摧毀整個星球的超級武器。

在沙漠星球塔圖因，一個年輕的農場男孩路克·天行者，夢想著能飛向星際，成為一名絕地武士，就像父親一樣。但他不知道，他的父親究竟是誰。

第二章　公主的求救

莱婭公主是反叛聯盟的秘密成員，她將絕地武士班·肯諾比的地址藏在機器人R2-D2裡，讓他尋找大師求救。

R2-D2和它的同伴C-3PO來到了塔圖因，遇到了路克。路克在機器人的記憶中，看到了公主求救的全息影像，心中激起了無法平息的波瀾。

第三章　原力的召喚

班·肯諾比告訴路克，他的父親是偉大的絕地武士，死於達斯·維德之手。他教路克感受原力，那種滲透在宇宙萬物中的神秘能量。

路克、漢·索羅和祝巴卡組成了奇怪的隊伍，踏上了拯救公主的旅途。他們不知道，這場冒險將決定整個銀河系的命運。
""");

        contents.put("原子習慣", """
第一章　細微改變的驚人力量

你以為只是多讀了一頁書，多走了幾步路，這有什麼意義？

如果你每天進步1%，一年後你會比現在好3700%。如果你每天退步1%，一年後你幾乎一無所有。這就是複利的力量，也是習慣的力量。

太多人在設定目標時，把眼光放在最終成果上：我要減掉20公斤，我要出版一本書，我要成為百萬富翁。但問題是，你每天實際上扮演的是什麼樣的角色？你的身份認同是什麼？

真正的改變來自於身份認同的改變，而不是目標的設定。

第二章　習慣的迴路

每個習慣都包含四個要素：提示、渴望、回應、獎勵。

提示觸發大腦去尋找獎勵。渴望是你想要的改變。回應是你實際執行的行動。獎勵強化了這個迴路，讓大腦記住這套模式。

要建立一個好習慣，你需要讓提示明顯，讓渴望有吸引力，讓回應容易，讓獎勵令人滿足。要打破壞習慣，就顛倒這四點。

第三章　兩分鐘法則

如果你想養成跑步的習慣，不要從「每天跑10公里」開始，從「穿上跑鞋走出門」開始。

這就是兩分鐘法則：任何習慣都應該能在兩分鐘內完成。它看起來很蠢，但目的不是完成那兩分鐘，而是讓自己開始。一旦開始了，往往就會繼續下去。

習慣的關鍵是出現，而不是完美。
""");

        contents.put("刻意練習", """
第一章　天才不存在的秘密

莫扎特六歲就能作曲，棋王卡斯帕羅夫在十三歲就擊敗了大師。他們是天才嗎？

研究顯示，這些「天才」背後都有一個共同點：大量的刻意練習。莫扎特的父親是一位音樂家，從他很小的時候就對他進行了密集的音樂訓練。卡斯帕羅夫從六歲開始下棋，在成為世界冠軍之前，已經積累了數萬小時的練習時間。

所謂天才，不過是比別人更早開始、更持久地進行正確練習的人。

第二章　刻意練習 vs. 天真練習

大多數人練習的方式是「天真練習」——機械地重複，把舒適帶當成進步。

刻意練習則不同。它要求你：走出舒適區，挑戰自己的極限；有明確的、具體的目標；得到即時的反饋；全神貫注，而不是心不在焉地重複。

就像鋼琴學生一遍遍彈同一首曲子，那不是刻意練習。刻意練習是找出自己彈錯的那個小節，重複練習直到正確，然後再放入整首曲子中。

第三章　心智表徵的建立

頂尖高手與普通人的根本區別，在於他們腦中的「心智表徵」——對某個領域的結構化認知圖像。

下棋大師看到一個棋盤，腦中不是一個個棋子，而是整體的戰略形勢，他能在幾秒內識別出數千種棋局模式。刻意練習的核心，就是建立越來越精細、越來越豐富的心智表徵，讓自己在關鍵時刻能夠更快、更準確地做出反應。
""");

        bookRepo.findAll().forEach(book -> {
            if (book.getContent() == null || book.getContent().isBlank()) {
                String content = contents.get(book.getTitle());
                if (content != null) {
                    book.setContent(content.strip());
                    bookRepo.save(book);
                }
            }
        });
    }

    private void createTopUpCodeIfAbsent(String code, BigDecimal amount) {
        topUpCodeRepo.findByCode(code).orElseGet(() -> topUpCodeRepo.save(new TopUpCode(code, amount)));
    }

    private void createPlatformWalletIfAbsent() {
        if (platformWalletRepo.count() == 0) {
            platformWalletRepo.save(new PlatformWallet(BigDecimal.ZERO));
        }
    }

    private void createPublisherWalletIfAbsent(AppUser publisher) {
        publisherWalletRepo.findByPublisher(publisher)
                .orElseGet(() -> publisherWalletRepo.save(new PublisherWallet(publisher)));
    }
}
