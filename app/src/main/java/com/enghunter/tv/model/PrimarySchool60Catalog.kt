package com.enghunter.tv.model

/**
 * 里程碑 M2：《小學英語核心 60 句高保真卡拉 OK 資源包》完整數據集
 * 覆蓋人教版 PEP / 外研版 小學 1~6 年級 6 大高頻交際主題，每個單元 10 個核心句式，共 60 句。
 */
object PrimarySchool60Catalog {

    private fun c(
        id: String,
        unit: CurriculumUnit,
        pattern: String,
        en: String,
        zh: String,
        emoji: String,
        chunks: List<String>,
        sub1: SlotOption,
        sub2: SlotOption,
        qEn: String,
        qZh: String
    ): SentenceCard {
        return SentenceCard(
            id = id,
            unitId = unit.unitId,
            unitTitle = unit.fullTitle,
            pattern = pattern,
            en = en,
            zh = zh,
            emoji = emoji,
            chunks = chunks,
            wordTimingsNormal = KaraokeTimingEstimator.buildTimingsForSentence(en, slowMode = false),
            wordTimingsSlow = KaraokeTimingEstimator.buildTimingsForSentence(en, slowMode = true),
            substitutions = listOf(sub1, sub2),
            rolePlayQuestion = qEn,
            rolePlayQuestionZh = qZh,
            prebakedAudioNormalAsset = "sentences/audio/${id}_normal.wav",
            prebakedAudioSlowAsset = "sentences/audio/${id}_slow.wav"
        )
    }

    val all60Sentences: List<SentenceCard> = listOf(
        // =====================================================================
        // Unit 1 · 禮貌問候與日常交際 (10 句)
        // =====================================================================
        c(
            id = "u01_s01", unit = CurriculumUnit.UNIT_1,
            pattern = "Hello! My name is ___.",
            en = "Hello! My name is Lily.",
            zh = "你好！我的名字叫莉莉。", emoji = "👋",
            chunks = listOf("Hello!", "My name", "is Lily."),
            sub1 = SlotOption("Tom", "汤姆", "👦", "Hello! My name is Tom.", "你好！我的名字叫汤姆。"),
            sub2 = SlotOption("Lucy", "露西", "👧", "Hello! My name is Lucy.", "你好！我的名字叫露西。"),
            qEn = "Hello! What is your name?", qZh = "你好！你叫什么名字？"
        ),
        c(
            id = "u01_s02", unit = CurriculumUnit.UNIT_1,
            pattern = "Nice to meet you, ___.",
            en = "Nice to meet you, too.",
            zh = "我也很高兴认识你。", emoji = "🤝",
            chunks = listOf("Nice to", "meet you,", "too."),
            sub1 = SlotOption("Miss White", "怀特老师", "👩‍🏫", "Nice to meet you, Miss White.", "很高兴认识您，怀特老师。"),
            sub2 = SlotOption("my friend", "我的朋友", "🧒", "Nice to meet you, my friend.", "很高兴认识你，我的朋友。"),
            qEn = "Nice to meet you!", qZh = "很高兴认识你！"
        ),
        c(
            id = "u01_s03", unit = CurriculumUnit.UNIT_1,
            pattern = "I am ___ years old.",
            en = "I am nine years old.",
            zh = "我今年九岁了。", emoji = "🎂",
            chunks = listOf("I am", "nine years", "old."),
            sub1 = SlotOption("eight", "八岁", "8️⃣", "I am eight years old.", "我今年八岁了。"),
            sub2 = SlotOption("ten", "十岁", "🔟", "I am ten years old.", "我今年十岁了。"),
            qEn = "How old are you?", qZh = "你几岁了？"
        ),
        c(
            id = "u01_s04", unit = CurriculumUnit.UNIT_1,
            pattern = "I am fine, thank you.",
            en = "I am fine, thank you.",
            zh = "我很好，谢谢你。", emoji = "😊",
            chunks = listOf("I am fine,", "thank", "you."),
            sub1 = SlotOption("very happy", "非常开心", "😄", "I am very happy, thank you.", "我非常开心，谢谢你。"),
            sub2 = SlotOption("doing great", "棒极了", "👍", "I am doing great, thank you.", "我棒极了，谢谢你。"),
            qEn = "How are you today?", qZh = "你今天过得怎么样？"
        ),
        c(
            id = "u01_s05", unit = CurriculumUnit.UNIT_1,
            pattern = "I am from ___.",
            en = "I am from Beijing, China.",
            zh = "我来自中国北京。", emoji = "🐼",
            chunks = listOf("I am", "from Beijing,", "China."),
            sub1 = SlotOption("Shanghai", "上海", "🏙️", "I am from Shanghai, China.", "我来自中国上海。"),
            sub2 = SlotOption("London", "伦敦", "🎡", "I am from London, UK.", "我来自英国伦敦。"),
            qEn = "Where are you from?", qZh = "你来自哪里？"
        ),
        c(
            id = "u01_s06", unit = CurriculumUnit.UNIT_1,
            pattern = "Good morning, ___!",
            en = "Good morning, dear teacher!",
            zh = "早上好，亲爱的老师！", emoji = "🌅",
            chunks = listOf("Good morning,", "dear", "teacher!"),
            sub1 = SlotOption("Mom and Dad", "爸爸妈妈", "👨‍👩‍👧", "Good morning, Mom and Dad!", "早上好，爸爸妈妈！"),
            sub2 = SlotOption("everyone", "大家", "🌞", "Good morning, everyone!", "大家早上好！"),
            qEn = "Good morning, class!", qZh = "同学们早上好！"
        ),
        c(
            id = "u01_s07", unit = CurriculumUnit.UNIT_1,
            pattern = "Thank you so much!",
            en = "Thank you for your help!",
            zh = "非常感谢你的帮助！", emoji = "💖",
            chunks = listOf("Thank you", "for your", "help!"),
            sub1 = SlotOption("your gift", "你的礼物", "🎁", "Thank you for your gift!", "谢谢你的礼物！"),
            sub2 = SlotOption("your book", "你的书", "📖", "Thank you for your book!", "谢谢你的书！"),
            qEn = "Here is a little gift for you.", qZh = "这是送给你的小礼物。"
        ),
        c(
            id = "u01_s08", unit = CurriculumUnit.UNIT_1,
            pattern = "You are very welcome.",
            en = "You are very welcome.",
            zh = "你太客气啦（不用谢）。", emoji = "🌟",
            chunks = listOf("You are", "very", "welcome."),
            sub1 = SlotOption("My pleasure", "我的荣幸", "✨", "It is my pleasure.", "这是我的荣幸。"),
            sub2 = SlotOption("No problem", "没问题", "👌", "No problem at all.", "完全没问题。"),
            qEn = "Thank you very much!", qZh = "非常感谢你！"
        ),
        c(
            id = "u01_s09", unit = CurriculumUnit.UNIT_1,
            pattern = "Excuse me, may I ___?",
            en = "Excuse me, may I come in?",
            zh = "打扰一下，我可以进来吗？", emoji = "🚪",
            chunks = listOf("Excuse me,", "may I", "come in?"),
            sub1 = SlotOption("ask a question", "问个问题", "🙋", "Excuse me, may I ask a question?", "打扰一下，我可以问个问题吗？"),
            sub2 = SlotOption("sit here", "坐这里", "🪑", "Excuse me, may I sit here?", "打扰一下，我可以坐这里吗？"),
            qEn = "Who is knocking at the door?", qZh = "谁在敲门呀？"
        ),
        c(
            id = "u01_s10", unit = CurriculumUnit.UNIT_1,
            pattern = "See you ___!",
            en = "Goodbye! See you tomorrow!",
            zh = "再见！我们明天见！", emoji = "👋",
            chunks = listOf("Goodbye!", "See you", "tomorrow!"),
            sub1 = SlotOption("next week", "下周", "📅", "Goodbye! See you next week!", "再见！我们下周见！"),
            sub2 = SlotOption("after school", "放学后", "🏫", "Goodbye! See you after school!", "再见！放学后见！"),
            qEn = "School is over. Goodbye!", qZh = "放学啦，再见！"
        ),

        // =====================================================================
        // Unit 2 · 美食點餐與禮貌請求 (10 句)
        // =====================================================================
        c(
            id = "u02_s01", unit = CurriculumUnit.UNIT_2,
            pattern = "Can I have some ___, please?",
            en = "Can I have some water, please?",
            zh = "请给我一些水好吗？", emoji = "💧",
            chunks = listOf("Can I have", "some water,", "please?"),
            sub1 = SlotOption("some bread", "一些面包", "🍞", "Can I have some bread, please?", "请给我一些面包好吗？"),
            sub2 = SlotOption("some milk", "一些牛奶", "🥛", "Can I have some milk, please?", "请给我一些牛奶好吗？"),
            qEn = "Welcome! What would you like?", qZh = "欢迎光临！你想要点什么？"
        ),
        c(
            id = "u02_s02", unit = CurriculumUnit.UNIT_2,
            pattern = "I would like ___.",
            en = "I would like a beef burger.",
            zh = "我想要一个牛肉汉堡。", emoji = "🍔",
            chunks = listOf("I would like", "a beef", "burger."),
            sub1 = SlotOption("some noodles", "一些面条", "🍜", "I would like some noodles.", "我想要一些面条。"),
            sub2 = SlotOption("an apple pie", "一个苹果派", "🥧", "I would like an apple pie.", "我想要一个苹果派。"),
            qEn = "Are you ready to order?", qZh = "你准备好点餐了吗？"
        ),
        c(
            id = "u02_s03", unit = CurriculumUnit.UNIT_2,
            pattern = "I like ___ very much.",
            en = "I like red apples very much.",
            zh = "我非常喜欢红苹果。", emoji = "🍎",
            chunks = listOf("I like", "red apples", "very much."),
            sub1 = SlotOption("sweet strawberries", "甜草莓", "🍓", "I like sweet strawberries very much.", "我非常喜欢甜草莓。"),
            sub2 = SlotOption("yellow bananas", "黄香蕉", "🍌", "I like yellow bananas very much.", "我非常喜欢黄香蕉。"),
            qEn = "What fruit do you like?", qZh = "你喜欢什么水果？"
        ),
        c(
            id = "u02_s04", unit = CurriculumUnit.UNIT_2,
            pattern = "My favorite food is ___.",
            en = "My favorite food is ice cream.",
            zh = "我最喜欢的食物是冰淇淋。", emoji = "🍦",
            chunks = listOf("My favorite", "food is", "ice cream."),
            sub1 = SlotOption("roast chicken", "烤鸡", "🍗", "My favorite food is roast chicken.", "我最喜欢的食物是烤鸡。"),
            sub2 = SlotOption("fresh fish", "鲜鱼", "🐟", "My favorite food is fresh fish.", "我最喜欢的食物是鲜鱼。"),
            qEn = "What is your favorite food?", qZh = "你最喜欢的食物是什么？"
        ),
        c(
            id = "u02_s05", unit = CurriculumUnit.UNIT_2,
            pattern = "Yes, please. / No, thank you.",
            en = "Yes, please. I love orange juice.",
            zh = "好的，麻烦了。我很喜欢橙汁。", emoji = "🧃",
            chunks = listOf("Yes, please.", "I love", "orange juice."),
            sub1 = SlotOption("green tea", "绿茶", "🍵", "Yes, please. I love green tea.", "好的，我很喜欢绿茶。"),
            sub2 = SlotOption("hot soup", "热汤", "🥣", "Yes, please. I love hot soup.", "好的，我很喜欢热汤。"),
            qEn = "Would you like some orange juice?", qZh = "你想喝点橙汁吗？"
        ),
        c(
            id = "u02_s06", unit = CurriculumUnit.UNIT_2,
            pattern = "Pass me the ___, please.",
            en = "Pass me the spoon, please.",
            zh = "请把勺子递给我。", emoji = "🥄",
            chunks = listOf("Pass me", "the spoon,", "please."),
            sub1 = SlotOption("chopsticks", "筷子", "🥢", "Pass me the chopsticks, please.", "请把筷子递给我。"),
            sub2 = SlotOption("dinner plate", "餐盘", "🍽️", "Pass me the dinner plate, please.", "请把餐盘递给我。"),
            qEn = "How can I help you at the table?", qZh = "餐桌上有什么需要帮你的？"
        ),
        c(
            id = "u02_s07", unit = CurriculumUnit.UNIT_2,
            pattern = "It tastes so ___!",
            en = "The cake tastes so delicious!",
            zh = "这块蛋糕尝起来太美味了！", emoji = "🍰",
            chunks = listOf("The cake", "tastes so", "delicious!"),
            sub1 = SlotOption("sweet and fresh", "又甜又新鲜", "🍉", "The watermelon tastes sweet and fresh!", "西瓜尝起来又甜又新鲜！"),
            sub2 = SlotOption("warm and nice", "又暖又香", "🥐", "The bread tastes warm and nice!", "面包尝起来又暖又香！"),
            qEn = "How does the cake taste?", qZh = "蛋糕尝起来味道怎么样？"
        ),
        c(
            id = "u02_s08", unit = CurriculumUnit.UNIT_2,
            pattern = "I am hungry / thirsty.",
            en = "Mom, I am a little hungry.",
            zh = "妈妈，我有一点饿了。", emoji = "😋",
            chunks = listOf("Mom,", "I am", "a little hungry."),
            sub1 = SlotOption("very thirsty", "非常渴", "🥤", "Mom, I am very thirsty.", "妈妈，我非常口渴。"),
            sub2 = SlotOption("full now", "吃饱了", "😌", "Mom, I am full now.", "妈妈，我现在吃饱啦。"),
            qEn = "Are you hungry after school?", qZh = "放学后你饿了吗？"
        ),
        c(
            id = "u02_s09", unit = CurriculumUnit.UNIT_2,
            pattern = "How much is ___?",
            en = "How much is this sandwich?",
            zh = "这个三明治多少钱？", emoji = "🥪",
            chunks = listOf("How much", "is this", "sandwich?"),
            sub1 = SlotOption("this milk", "这盒牛奶", "🥛", "How much is this milk?", "这盒牛奶多少钱？"),
            sub2 = SlotOption("that cake", "那个蛋糕", "🎂", "How much is that cake?", "那个蛋糕多少钱？"),
            qEn = "Can I help you?", qZh = "请问需要买点什么？"
        ),
        c(
            id = "u02_s10", unit = CurriculumUnit.UNIT_2,
            pattern = "Here you are.",
            en = "Here is your hot breakfast.",
            zh = "给你热腾腾的早餐。", emoji = "🍳",
            chunks = listOf("Here is", "your hot", "breakfast."),
            sub1 = SlotOption("fresh salad", "新鲜沙拉", "🥗", "Here is your fresh salad.", "给你新鲜的沙拉。"),
            sub2 = SlotOption("lunch box", "午餐盒", "🍱", "Here is your lunch box.", "给你准备好的午餐盒。"),
            qEn = "Where is my breakfast?", qZh = "我的早餐在哪里呀？"
        ),

        // =====================================================================
        // Unit 3 · 校園生活與文具物品 (10 句)
        // =====================================================================
        c(
            id = "u03_s01", unit = CurriculumUnit.UNIT_3,
            pattern = "I have a new ___.",
            en = "I have a new schoolbag.",
            zh = "我有一个新书包。", emoji = "🎒",
            chunks = listOf("I have", "a new", "schoolbag."),
            sub1 = SlotOption("pencil box", "铅笔盒", "✏️", "I have a new pencil box.", "我有一个新铅笔盒。"),
            sub2 = SlotOption("story book", "故事书", "📗", "I have a new story book.", "我有一本新故事书。"),
            qEn = "What do you have in your hand?", qZh = "你手里拿着什么？"
        ),
        c(
            id = "u03_s02", unit = CurriculumUnit.UNIT_3,
            pattern = "May I borrow your ___?",
            en = "May I borrow your ruler, please?",
            zh = "请问我可以借用一下你的尺子吗？", emoji = "📏",
            chunks = listOf("May I borrow", "your ruler,", "please?"),
            sub1 = SlotOption("eraser", "橡皮", "🧼", "May I borrow your eraser, please?", "请问我可以借用你的橡皮吗？"),
            sub2 = SlotOption("blue pen", "蓝钢笔", "🖊️", "May I borrow your blue pen, please?", "请问我可以借用你的蓝钢笔吗？"),
            qEn = "Sure! What do you need?", qZh = "当然可以！你需要什么？"
        ),
        c(
            id = "u03_s03", unit = CurriculumUnit.UNIT_3,
            pattern = "Where is my ___?",
            en = "Where is my English book?",
            zh = "我的英语书在哪里？", emoji = "📘",
            chunks = listOf("Where is", "my English", "book?"),
            sub1 = SlotOption("red crayon", "红蜡笔", "🖍️", "Where is my red crayon?", "我的红蜡笔在哪里？"),
            sub2 = SlotOption("notebook", "笔记本", "📓", "Where is my notebook?", "我的笔记本在哪里？"),
            qEn = "Look! It is on the desk.", qZh = "看！它在书桌上。"
        ),
        c(
            id = "u03_s04", unit = CurriculumUnit.UNIT_3,
            pattern = "It is in / on / under the ___.",
            en = "It is under the teacher's desk.",
            zh = "它在讲台桌子的下面。", emoji = "🏫",
            chunks = listOf("It is", "under the", "teacher's desk."),
            sub1 = SlotOption("in your bag", "在你书包里", "🎒", "It is in your schoolbag.", "它在你的书包里面。"),
            sub2 = SlotOption("on the chair", "在椅子上", "🪑", "It is on the wooden chair.", "它在木椅子上面。"),
            qEn = "Where is the ball?", qZh = "球在哪里呀？"
        ),
        c(
            id = "u03_s05", unit = CurriculumUnit.UNIT_3,
            pattern = "My favorite subject is ___.",
            en = "My favorite subject is English.",
            zh = "我最喜欢的科目是英语。", emoji = "🔤",
            chunks = listOf("My favorite", "subject is", "English."),
            sub1 = SlotOption("music class", "音乐课", "🎵", "My favorite subject is music.", "我最喜欢的科目是音乐。"),
            sub2 = SlotOption("science", "科学课", "🔬", "My favorite subject is science.", "我最喜欢的科目是科学。"),
            qEn = "What subject do you like best?", qZh = "你最喜欢哪门课？"
        ),
        c(
            id = "u03_s06", unit = CurriculumUnit.UNIT_3,
            pattern = "Let's clean the ___!",
            en = "Let's clean the classroom together!",
            zh = "让我们一起打扫教室吧！", emoji = "🧹",
            chunks = listOf("Let's clean", "the classroom", "together!"),
            sub1 = SlotOption("blackboard", "黑板", "🧽", "Let's clean the blackboard together!", "让我们一起擦黑板吧！"),
            sub2 = SlotOption("windows", "窗户", "🪟", "Let's clean the windows together!", "让我们一起擦窗户吧！"),
            qEn = "Classroom duty time!", qZh = "现在是班级值日时间！"
        ),
        c(
            id = "u03_s07", unit = CurriculumUnit.UNIT_3,
            pattern = "Open your ___, please.",
            en = "Open your books to page ten.",
            zh = "请把书翻到第十页。", emoji = "📖",
            chunks = listOf("Open your", "books to", "page ten."),
            sub1 = SlotOption("page twenty", "第二十页", "📑", "Open your books to page twenty.", "请把书翻到第二十页。"),
            sub2 = SlotOption("unit three", "第三单元", "📚", "Open your books to unit three.", "请把书翻到第三单元。"),
            qEn = "Are you ready for class?", qZh = "准备好上课了吗？"
        ),
        c(
            id = "u03_s08", unit = CurriculumUnit.UNIT_3,
            pattern = "Is this your ___?",
            en = "Yes, this is my dictionary.",
            zh = "是的，这是我的词典。", emoji = "📕",
            chunks = listOf("Yes,", "this is", "my dictionary."),
            sub1 = SlotOption("water bottle", "水杯", "🍼", "Yes, this is my water bottle.", "是的，这是我的水杯。"),
            sub2 = SlotOption("umbrella", "雨伞", "☂️", "Yes, this is my umbrella.", "是的，这是我的雨伞。"),
            qEn = "Is this your dictionary?", qZh = "这是你的词典吗？"
        ),
        c(
            id = "u03_s09", unit = CurriculumUnit.UNIT_3,
            pattern = "We have ___ today.",
            en = "We have art and music today.",
            zh = "我们今天有美术课和音乐课。", emoji = "🎨",
            chunks = listOf("We have", "art and music", "today."),
            sub1 = SlotOption("math and PE", "数学与体育", "🏃", "We have math and PE today.", "我们今天有数学课和体育课。"),
            sub2 = SlotOption("computer class", "电脑课", "💻", "We have computer class today.", "我们今天有电脑课。"),
            qEn = "What classes do we have today?", qZh = "我们今天有什么课？"
        ),
        c(
            id = "u03_s10", unit = CurriculumUnit.UNIT_3,
            pattern = "Listen to the teacher ___.",
            en = "Listen to the teacher carefully.",
            zh = "请认真听老师讲课。", emoji = "👂",
            chunks = listOf("Listen to", "the teacher", "carefully."),
            sub1 = SlotOption("read aloud", "大声朗读", "🗣️", "Read the English text aloud.", "请大声朗读英语课文。"),
            sub2 = SlotOption("raise your hand", "举手发言", "✋", "Please raise your hand first.", "发言前请先举手。"),
            qEn = "What is our class rule?", qZh = "我们的课堂规则是什么？"
        ),

        // =====================================================================
        // Unit 4 · 家庭成員與興趣運動 (10 句)
        // =====================================================================
        c(
            id = "u04_s01", unit = CurriculumUnit.UNIT_4,
            pattern = "This is my ___.",
            en = "This is my loving family.",
            zh = "这是我充满爱的家庭。", emoji = "👨‍👩‍👧‍👦",
            chunks = listOf("This is", "my loving", "family."),
            sub1 = SlotOption("kind father", "和蔼的爸爸", "👨", "This is my kind father.", "这是我和蔼的爸爸。"),
            sub2 = SlotOption("pretty mother", "漂亮的妈妈", "👩", "This is my pretty mother.", "这是我漂亮的妈妈。"),
            qEn = "Who are they in the photo?", qZh = "照片里的人是谁呀？"
        ),
        c(
            id = "u04_s02", unit = CurriculumUnit.UNIT_4,
            pattern = "He / She is a ___.",
            en = "My father is a busy doctor.",
            zh = "我的爸爸是一名忙碌的医生。", emoji = "🩺",
            chunks = listOf("My father", "is a", "busy doctor."),
            sub1 = SlotOption("great teacher", "优秀的老师", "👩‍🏫", "My mother is a great teacher.", "我的妈妈是一名优秀的老师。"),
            sub2 = SlotOption("brave driver", "勇敢的司机", "🚌", "My uncle is a brave driver.", "我的叔叔是一名勇敢的司机。"),
            qEn = "What does your father do?", qZh = "你爸爸是做什么工作的？"
        ),
        c(
            id = "u04_s03", unit = CurriculumUnit.UNIT_4,
            pattern = "I can ___ very well.",
            en = "I can play basketball very well.",
            zh = "我篮球打得非常好。", emoji = "🏀",
            chunks = listOf("I can", "play basketball", "very well."),
            sub1 = SlotOption("swim fast", "游得很快", "🏊", "I can swim very fast.", "我游泳游得非常快。"),
            sub2 = SlotOption("sing English songs", "唱英文歌", "🎤", "I can sing English songs well.", "我英文歌唱得很好。"),
            qEn = "What sports can you do?", qZh = "你会什么运动或才艺？"
        ),
        c(
            id = "u04_s04", unit = CurriculumUnit.UNIT_4,
            pattern = "Can you play ___?",
            en = "Yes, I can play football.",
            zh = "是的，我会踢足球。", emoji = "⚽",
            chunks = listOf("Yes,", "I can", "play football."),
            sub1 = SlotOption("ping-pong", "乒乓球", "🏓", "Yes, I can play ping-pong.", "是的，我会打乒乓球。"),
            sub2 = SlotOption("the piano", "弹钢琴", "🎹", "Yes, I can play the piano.", "是的，我会弹钢琴。"),
            qEn = "Can you play football with me?", qZh = "你能和我一起踢足球吗？"
        ),
        c(
            id = "u04_s05", unit = CurriculumUnit.UNIT_4,
            pattern = "My hobby is ___.",
            en = "I like reading storybooks at home.",
            zh = "我喜欢在家里读故事书。", emoji = "📚",
            chunks = listOf("I like", "reading storybooks", "at home."),
            sub1 = SlotOption("drawing pictures", "画画", "🎨", "I like drawing cartoons at home.", "我喜欢在家里画卡通画。"),
            sub2 = SlotOption("riding a bike", "骑自行车", "🚲", "I like riding my bike in the park.", "我喜欢在公园里骑自行车。"),
            qEn = "What is your hobby?", qZh = "你的业余爱好是什么？"
        ),
        c(
            id = "u04_s06", unit = CurriculumUnit.UNIT_4,
            pattern = "Let's ___ after school!",
            en = "Let's play badminton after school!",
            zh = "放学后我们一起打羽毛球吧！", emoji = "🏸",
            chunks = listOf("Let's play", "badminton", "after school!"),
            sub1 = SlotOption("fly a kite", "放风筝", "🪁", "Let's fly a kite in the park!", "我们去公园放风筝吧！"),
            sub2 = SlotOption("jump rope", "跳绳", "🪢", "Let's jump rope on the playground!", "我们在操场上跳绳吧！"),
            qEn = "What shall we do after school?", qZh = "放学后我们玩什么好呢？"
        ),
        c(
            id = "u04_s07", unit = CurriculumUnit.UNIT_4,
            pattern = "There are ___ people in my family.",
            en = "There are four people in my family.",
            zh = "我家有四口人。", emoji = "🏠",
            chunks = listOf("There are", "four people", "in my family."),
            sub1 = SlotOption("three people", "三口人", "👪", "There are three people in my family.", "我家有三口人。"),
            sub2 = SlotOption("five people", "五口人", "👨‍👩‍👧‍👦", "There are five people in my family.", "我家有五口人。"),
            qEn = "How many people are there in your family?", qZh = "你家里有几口人？"
        ),
        c(
            id = "u04_s08", unit = CurriculumUnit.UNIT_4,
            pattern = "I often help my mom ___.",
            en = "I often help my mom wash dishes.",
            zh = "我经常帮妈妈洗碗。", emoji = "🍽️",
            chunks = listOf("I often", "help my mom", "wash dishes."),
            sub1 = SlotOption("sweep the floor", "扫地", "🧹", "I often help my mom sweep the floor.", "我经常帮妈妈扫地。"),
            sub2 = SlotOption("water the flowers", "浇花", "🌻", "I often help my mom water the flowers.", "我经常帮妈妈浇花。"),
            qEn = "Are you helpful at home?", qZh = "你在家里会帮爸妈做家务吗？"
        ),
        c(
            id = "u04_s09", unit = CurriculumUnit.UNIT_4,
            pattern = "She has ___ hair and big eyes.",
            en = "My sister has long hair and big eyes.",
            zh = "我的妹妹有一头长发和大眼睛。", emoji = "👧",
            chunks = listOf("My sister has", "long hair", "and big eyes."),
            sub1 = SlotOption("short hair", "短发", "👦", "My brother has short hair and bright eyes.", "我弟弟留着短发，有一双明亮的眼睛。"),
            sub2 = SlotOption("cute glasses", "可爱的眼镜", "🤓", "My friend wears cute glasses.", "我的朋友戴着可爱的眼镜。"),
            qEn = "What does your sister look like?", qZh = "你妹妹长什么样？"
        ),
        c(
            id = "u04_s10", unit = CurriculumUnit.UNIT_4,
            pattern = "We watch TV in the ___.",
            en = "We play games in the living room.",
            zh = "我们在客厅里一起做游戏。", emoji = "🛋️",
            chunks = listOf("We play games", "in the", "living room."),
            sub1 = SlotOption("read in the study", "在书房看书", "📖", "We read books in the quiet study.", "我们在安静的书房里看书。"),
            sub2 = SlotOption("cook in the kitchen", "在厨房做饭", "🍳", "We make dinner in the kitchen.", "我们在厨房里做晚饭。"),
            qEn = "Where are you playing now?", qZh = "你们现在在哪个房间玩？"
        ),

        // =====================================================================
        // Unit 5 · 日常作息與天氣穿搭 (10 句)
        // =====================================================================
        c(
            id = "u05_s01", unit = CurriculumUnit.UNIT_5,
            pattern = "It is time to ___.",
            en = "It is time to go to school.",
            zh = "到去上学的时间了。", emoji = "🎒",
            chunks = listOf("It is time", "to go", "to school."),
            sub1 = SlotOption("have dinner", "吃晚饭", "🍲", "It is time to have dinner.", "到吃晚饭的时间了。"),
            sub2 = SlotOption("go to bed", "上床睡觉", "🛏️", "It is time to go to bed.", "到上床睡觉的时间了。"),
            qEn = "What time is it now?", qZh = "现在几点啦？"
        ),
        c(
            id = "u05_s02", unit = CurriculumUnit.UNIT_5,
            pattern = "I get up at ___ o'clock.",
            en = "I get up at seven o'clock.",
            zh = "我早上七点钟起床。", emoji = "⏰",
            chunks = listOf("I get up", "at seven", "o'clock."),
            sub1 = SlotOption("do homework at five", "五点写作业", "📝", "I do my homework at five o'clock.", "我下午五点钟写作业。"),
            sub2 = SlotOption("sleep at nine", "九点睡觉", "🌙", "I go to sleep at nine o'clock.", "我晚上九点钟睡觉。"),
            qEn = "When do you get up every day?", qZh = "你每天几点起床？"
        ),
        c(
            id = "u05_s03", unit = CurriculumUnit.UNIT_5,
            pattern = "It is ___ and ___ today.",
            en = "It is sunny and warm today.",
            zh = "今天天气晴朗又温暖。", emoji = "☀️",
            chunks = listOf("It is", "sunny and warm", "today."),
            sub1 = SlotOption("windy and cool", "有风又凉爽", "🍃", "It is windy and cool today.", "今天有风又凉爽。"),
            sub2 = SlotOption("snowy and cold", "下雪又寒冷", "❄️", "It is snowy and cold today.", "今天下雪又寒冷。"),
            qEn = "What is the weather like today?", qZh = "今天天气怎么样？"
        ),
        c(
            id = "u05_s04", unit = CurriculumUnit.UNIT_5,
            pattern = "Put on your ___.",
            en = "It is cold. Put on your coat.",
            zh = "天冷了，穿上你的外套吧。", emoji = "🧥",
            chunks = listOf("It is cold.", "Put on", "your coat."),
            sub1 = SlotOption("warm hat", "暖和的帽子", "🧢", "It is cold. Put on your warm hat.", "天冷了，戴上你暖和的帽子吧。"),
            sub2 = SlotOption("rain boots", "雨靴", "🥾", "It is raining. Put on your rain boots.", "下雨了，穿上你的雨靴吧。"),
            qEn = "It is windy outside. What should I wear?", qZh = "外面风大，我该穿什么？"
        ),
        c(
            id = "u05_s05", unit = CurriculumUnit.UNIT_5,
            pattern = "I like ___ best.",
            en = "I like spring because of the flowers.",
            zh = "我最喜欢春天，因为百花盛开。", emoji = "🌸",
            chunks = listOf("I like spring", "because of", "the flowers."),
            sub1 = SlotOption("summer / swim", "夏天可以游泳", "🏖️", "I like summer because I can swim.", "我喜欢夏天，因为我可以去游泳。"),
            sub2 = SlotOption("winter / snowman", "冬天堆雪人", "⛄", "I like winter because of the snow.", "我喜欢冬天，因为可以玩雪。"),
            qEn = "Which season do you like best?", qZh = "你最喜欢哪个季节？"
        ),
        c(
            id = "u05_s06", unit = CurriculumUnit.UNIT_5,
            pattern = "Whose ___ is this?",
            en = "This blue sweater is mine.",
            zh = "这件蓝色的毛衣是我的。", emoji = "🧶",
            chunks = listOf("This blue", "sweater", "is mine."),
            sub1 = SlotOption("white shirt", "白衬衫", "👔", "This white shirt is my father's.", "这件白衬衫是我爸爸的。"),
            sub2 = SlotOption("pink dress", "粉裙子", "👗", "This pink dress is my sister's.", "这条粉色连衣裙是我妹妹的。"),
            qEn = "Whose blue sweater is this?", qZh = "这件蓝色毛衣是谁的？"
        ),
        c(
            id = "u05_s07", unit = CurriculumUnit.UNIT_5,
            pattern = "What day is it today?",
            en = "Today is Friday. Tomorrow is Saturday!",
            zh = "今天是星期五，明天就是星期六啦！", emoji = "🎉",
            chunks = listOf("Today is Friday.", "Tomorrow is", "Saturday!"),
            sub1 = SlotOption("Monday", "星期一", "📅", "Today is Monday. Let's work hard!", "今天是星期一，我们努力学习吧！"),
            sub2 = SlotOption("Sunday", "星期日", "🎈", "Today is Sunday. Let's go out!", "今天是星期日，我们出去玩吧！"),
            qEn = "What day is it today?", qZh = "今天是星期几？"
        ),
        c(
            id = "u05_s08", unit = CurriculumUnit.UNIT_5,
            pattern = "Don't forget your ___!",
            en = "It is rainy. Take your umbrella!",
            zh = "外面下雨了，记得带上你的雨伞！", emoji = "☔",
            chunks = listOf("It is rainy.", "Take your", "umbrella!"),
            sub1 = SlotOption("sunglasses", "太阳镜", "🕶️", "It is sunny. Take your sunglasses!", "太阳很大，带上你的太阳镜！"),
            sub2 = SlotOption("water bottle", "水壶", "🍼", "It is hot. Take your water bottle!", "天气很热，带上你的水壶！"),
            qEn = "I am going outside now, Mom!", qZh = "妈妈，我现在要出门啦！"
        ),
        c(
            id = "u05_s09", unit = CurriculumUnit.UNIT_5,
            pattern = "I brush my teeth ___.",
            en = "I brush my teeth every morning.",
            zh = "我每天早上都会刷牙。", emoji = "🪥",
            chunks = listOf("I brush", "my teeth", "every morning."),
            sub1 = SlotOption("wash my face", "洗脸", "🧼", "I wash my face every morning.", "我每天早上都会洗脸。"),
            sub2 = SlotOption("comb my hair", "梳头", "🪮", "I comb my hair every morning.", "我每天早上都会梳头。"),
            qEn = "What do you do after getting up?", qZh = "你起床后会做什么？"
        ),
        c(
            id = "u05_s10", unit = CurriculumUnit.UNIT_5,
            pattern = "Have a good ___!",
            en = "Good night! Sweet dreams to you!",
            zh = "晚安！祝你做个甜甜的美梦！", emoji = "🌟",
            chunks = listOf("Good night!", "Sweet dreams", "to you!"),
            sub1 = SlotOption("Have a nice weekend", "周末愉快", "🎡", "Have a wonderful weekend!", "祝你度过一个愉快的周末！"),
            sub2 = SlotOption("Have a good trip", "旅途愉快", "✈️", "Have a safe and happy trip!", "祝你旅途平安愉快！"),
            qEn = "Time for bed. Good night!", qZh = "该睡觉啦，晚安！"
        ),

        // =====================================================================
        // Unit 6 · 動物世界與問路出行 (10 句)
        // =====================================================================
        c(
            id = "u06_s01", unit = CurriculumUnit.UNIT_6,
            pattern = "Look at that ___!",
            en = "Look at that cute panda!",
            zh = "快看那只可爱的大熊猫！", emoji = "🐼",
            chunks = listOf("Look at", "that cute", "panda!"),
            sub1 = SlotOption("tall giraffe", "高高的长颈鹿", "🦒", "Look at that tall giraffe!", "快看那只高高的长颈鹿！"),
            sub2 = SlotOption("clever monkey", "聪明的小猴子", "🐒", "Look at that clever monkey!", "快看那只聪明的小猴子！"),
            qEn = "What do you see in the zoo?", qZh = "你在动物园里看到了什么？"
        ),
        c(
            id = "u06_s02", unit = CurriculumUnit.UNIT_6,
            pattern = "It has a ___ nose / tail.",
            en = "The elephant has a long nose.",
            zh = "大象有一条长长的鼻子。", emoji = "🐘",
            chunks = listOf("The elephant", "has a", "long nose."),
            sub1 = SlotOption("rabbit / long ears", "兔子长耳朵", "🐰", "The rabbit has two long ears.", "小兔子有两只长长的耳朵。"),
            sub2 = SlotOption("bird / colorful wings", "彩色翅膀", "🦜", "The bird has colorful wings.", "这只小鸟有五彩缤纷的翅膀。"),
            qEn = "Why is the elephant so special?", qZh = "大象为什么这么特别？"
        ),
        c(
            id = "u06_s03", unit = CurriculumUnit.UNIT_6,
            pattern = "I go to school by ___.",
            en = "I go to school by bus.",
            zh = "我乘公共汽车去上学。", emoji = "🚌",
            chunks = listOf("I go", "to school", "by bus."),
            sub1 = SlotOption("by subway", "乘地铁", "🚇", "I go to school by subway.", "我乘地铁去上学。"),
            sub2 = SlotOption("on foot", "步行", "🚶", "I walk to school every day.", "我每天步行去上学。"),
            qEn = "How do you go to school?", qZh = "你平时怎么去上学？"
        ),
        c(
            id = "u06_s04", unit = CurriculumUnit.UNIT_6,
            pattern = "Stop and wait at a ___ light.",
            en = "Stop and wait at a red light.",
            zh = "红灯亮时请停下来等待。", emoji = "🚦",
            chunks = listOf("Stop and wait", "at a", "red light."),
            sub1 = SlotOption("green light / go", "绿灯行", "🟢", "Go at a green traffic light.", "绿灯亮时可以安全通行。"),
            sub2 = SlotOption("yellow light / slow", "黄灯减速", "🟡", "Slow down at a yellow light.", "黄灯亮时请减速慢行。"),
            qEn = "What is the traffic rule?", qZh = "过马路的交通规则是什么？"
        ),
        c(
            id = "u06_s05", unit = CurriculumUnit.UNIT_6,
            pattern = "Where is the ___?",
            en = "Excuse me, where is the library?",
            zh = "打扰一下，请问图书馆在哪里？", emoji = "🏛️",
            chunks = listOf("Excuse me,", "where is", "the library?"),
            sub1 = SlotOption("city park", "城市公园", "🏞️", "Excuse me, where is the city park?", "打扰一下，请问城市公园在哪里？"),
            sub2 = SlotOption("post office", "邮局", "📮", "Excuse me, where is the post office?", "打扰一下，请问邮局在哪里？"),
            qEn = "How can I help you, little boy?", qZh = "小朋友，有什么可以帮你的吗？"
        ),
        c(
            id = "u06_s06", unit = CurriculumUnit.UNIT_6,
            pattern = "Go straight and turn ___.",
            en = "Go straight and turn left here.",
            zh = "一直往前走，然后在这里左转。", emoji = "⬅️",
            chunks = listOf("Go straight", "and turn", "left here."),
            sub1 = SlotOption("turn right", "向右转", "➡️", "Go straight and turn right at the corner.", "直走然后在路口向右转。"),
            sub2 = SlotOption("next to the hospital", "在医院旁边", "🏥", "It is right next to the hospital.", "它就在医院的旁边。"),
            qEn = "How can I get to the bookstore?", qZh = "请问去书店怎么走？"
        ),
        c(
            id = "u06_s07", unit = CurriculumUnit.UNIT_6,
            pattern = "I am going to ___ this weekend.",
            en = "I am going to visit my grandparents.",
            zh = "这个周末我要去看望爷爷奶奶。", emoji = "👵",
            chunks = listOf("I am going", "to visit", "my grandparents."),
            sub1 = SlotOption("take a trip", "去旅行", "🚂", "I am going to take a train trip.", "我要坐火车去旅行。"),
            sub2 = SlotOption("see a film", "看电影", "🎬", "I am going to see a funny film.", "我要去看一场有趣的电影。"),
            qEn = "What are you going to do this weekend?", qZh = "你这个周末打算做什么？"
        ),
        c(
            id = "u06_s08", unit = CurriculumUnit.UNIT_6,
            pattern = "How many ___ can you see?",
            en = "I can see twelve white sheep.",
            zh = "我能看到十二只白色的小绵羊。", emoji = "🐑",
            chunks = listOf("I can see", "twelve white", "sheep."),
            sub1 = SlotOption("fifteen ducks", "十五只鸭子", "🦆", "I can see fifteen yellow ducks.", "我能看到十五只小黄鸭。"),
            sub2 = SlotOption("twenty horses", "二十匹马", "🐎", "I can see twenty strong horses.", "我能看到二十匹强壮的骏马。"),
            qEn = "How many sheep are there on the farm?", qZh = "农场里有多少只绵羊？"
        ),
        c(
            id = "u06_s09", unit = CurriculumUnit.UNIT_6,
            pattern = "What are the ___ doing?",
            en = "The birds are singing in the tree.",
            zh = "小鸟们正在树上唱歌呢。", emoji = "🐦",
            chunks = listOf("The birds", "are singing", "in the tree."),
            sub1 = SlotOption("rabbits / jumping", "兔子在跳", "🐇", "The rabbits are jumping on the grass.", "小兔子们正在草地上蹦蹦跳跳。"),
            sub2 = SlotOption("fish / swimming", "鱼儿在游", "🐠", "The fish are swimming in the river.", "小鱼们正在河里游来游去。"),
            qEn = "Listen! What are the birds doing?", qZh = "听！小鸟们在做什么？"
        ),
        c(
            id = "u06_s10", unit = CurriculumUnit.UNIT_6,
            pattern = "Let's protect the ___!",
            en = "Animals are our good friends.",
            zh = "动物是我们的好朋友。", emoji = "🌍",
            chunks = listOf("Animals are", "our good", "friends."),
            sub1 = SlotOption("keep the park clean", "保持公园整洁", "🌳", "Let's keep the green park clean!", "让我们保持绿色公园的整洁！"),
            sub2 = SlotOption("love nature", "热爱大自然", "🌿", "Let's love and protect nature!", "让我们热爱并保护大自然！"),
            qEn = "What did you learn at the zoo today?", qZh = "今天在动物园你学到了什么？"
        )
    )
}
