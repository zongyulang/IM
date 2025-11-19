package com.vim.webpage.Utils;

import com.vim.webpage.domain.User;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户数据打包工具类
 * 用于根据语言处理用户数据的多语言字段
 */
public class UserDataPackageUtil {

    // 家乡翻译映射表
    private static final Map<String, Map<String, String>> HOMETOWN_TRANSLATIONS = new HashMap<>();

    static {
        // 简体中文
        Map<String, String> zhcn = new HashMap<>();
        zhcn.put("越南", "越南");
        zhcn.put("泰国", "泰国");
        zhcn.put("中国", "中国");
        zhcn.put("台湾", "台湾");
        zhcn.put("日本", "日本");
        zhcn.put("香港", "香港");
        zhcn.put("韩国", "韩国");
        zhcn.put("新加坡", "新加坡");
        zhcn.put("马来西亚", "马来西亚");
        zhcn.put("菲律宾", "菲律宾");
        zhcn.put("印度尼西亚", "印度尼西亚");
        zhcn.put("印度", "印度");
        zhcn.put("美国", "美国");
        zhcn.put("加拿大", "加拿大");
        zhcn.put("英国", "英国");
        zhcn.put("法国", "法国");
        zhcn.put("德国", "德国");
        zhcn.put("西班牙", "西班牙");
        zhcn.put("意大利", "意大利");
        zhcn.put("俄罗斯", "俄罗斯");
        zhcn.put("澳大利亚", "澳大利亚");
        zhcn.put("新西兰", "新西兰");
        zhcn.put("巴西", "巴西");
        zhcn.put("阿根廷", "阿根廷");
        zhcn.put("墨西哥", "墨西哥");
        zhcn.put("南非", "南非");
        zhcn.put("未知", "其他");
        HOMETOWN_TRANSLATIONS.put("zhcn", zhcn);
        HOMETOWN_TRANSLATIONS.put("ZHCN", zhcn);

        // 繁体中文
        Map<String, String> zhtw = new HashMap<>();
        zhtw.put("越南", "越南");
        zhtw.put("泰国", "泰国");
        zhtw.put("中国", "中國");
        zhtw.put("台湾", "台灣");
        zhtw.put("日本", "日本");
        zhtw.put("香港", "香港");
        zhtw.put("韩国", "韓國");
        zhtw.put("新加坡", "新加坡");
        zhtw.put("马来西亚", "馬來西亞");
        zhtw.put("菲律宾", "菲律賓");
        zhtw.put("印度尼西亚", "印尼");
        zhtw.put("印度", "印度");
        zhtw.put("美国", "美國");
        zhtw.put("加拿大", "加拿大");
        zhtw.put("英国", "英國");
        zhtw.put("法国", "法國");
        zhtw.put("德国", "德國");
        zhtw.put("西班牙", "西班牙");
        zhtw.put("意大利", "意大利");
        zhtw.put("俄罗斯", "俄羅斯");
        zhtw.put("澳大利亚", "澳洲");
        zhtw.put("新西兰", "紐西蘭");
        zhtw.put("巴西", "巴西");
        zhtw.put("阿根廷", "阿根廷");
        zhtw.put("墨西哥", "墨西哥");
        zhtw.put("南非", "南非");
        zhtw.put("未知", "其他");
        HOMETOWN_TRANSLATIONS.put("zhtw", zhtw);
        HOMETOWN_TRANSLATIONS.put("ZHTW", zhtw);

        // 英文
        Map<String, String> enus = new HashMap<>();
        enus.put("越南", "Vietnam");
        enus.put("泰国", "Thailand");
        enus.put("中国", "China");
        enus.put("台湾", "Taiwan");
        enus.put("日本", "Japan");
        enus.put("香港", "Hong Kong");
        enus.put("韩国", "South Korea");
        enus.put("新加坡", "Singapore");
        enus.put("马来西亚", "Malaysia");
        enus.put("菲律宾", "Philippines");
        enus.put("印度尼西亚", "Indonesia");
        enus.put("印度", "India");
        enus.put("美国", "United States");
        enus.put("加拿大", "Canada");
        enus.put("英国", "United Kingdom");
        enus.put("法国", "France");
        enus.put("德国", "Germany");
        enus.put("西班牙", "Spain");
        enus.put("意大利", "Italy");
        enus.put("俄罗斯", "Russia");
        enus.put("澳大利亚", "Australia");
        enus.put("新西兰", "New Zealand");
        enus.put("巴西", "Brazil");
        enus.put("阿根廷", "Argentina");
        enus.put("墨西哥", "Mexico");
        enus.put("南非", "South Africa");
        enus.put("未知", "Other");
        HOMETOWN_TRANSLATIONS.put("enus", enus);
        HOMETOWN_TRANSLATIONS.put("ENUS", enus);

        // 日文
        Map<String, String> jajp = new HashMap<>();
        jajp.put("越南", "ベトナム");
        jajp.put("泰国", "タイ");
        jajp.put("中国", "中国");
        jajp.put("台湾", "台湾");
        jajp.put("日本", "日本");
        jajp.put("香港", "香港");
        jajp.put("韩国", "韓国");
        jajp.put("新加坡", "シンガポール");
        jajp.put("马来西亚", "マレーシア");
        jajp.put("菲律宾", "フィリピン");
        jajp.put("印度尼西亚", "インドネシア");
        jajp.put("印度", "インド");
        jajp.put("美国", "アメリカ合衆国");
        jajp.put("加拿大", "カナダ");
        jajp.put("英国", "イギリス");
        jajp.put("法国", "フランス");
        jajp.put("德国", "ドイツ");
        jajp.put("西班牙", "スペイン");
        jajp.put("意大利", "イタリア");
        jajp.put("俄罗斯", "ロシア");
        jajp.put("澳大利亚", "オーストラリア");
        jajp.put("新西兰", "ニュージーランド");
        jajp.put("巴西", "ブラジル");
        jajp.put("阿根廷", "アルゼンチン");
        jajp.put("墨西哥", "メキシコ");
        jajp.put("南非", "南アフリカ共和国");
        jajp.put("其他", "その他");
        HOMETOWN_TRANSLATIONS.put("jajp", jajp);
        HOMETOWN_TRANSLATIONS.put("JAJP", jajp);

        // 韩文
        Map<String, String> kokr = new HashMap<>();
        kokr.put("越南", "베트남");
        kokr.put("泰国", "태국");
        kokr.put("中国", "중국");
        kokr.put("台湾", "대만");
        kokr.put("日本", "일본");
        kokr.put("香港", "홍콩");
        kokr.put("韩国", "한국");
        kokr.put("新加坡", "싱가포르");
        kokr.put("马来西亚", "말레이시아");
        kokr.put("菲律宾", "필리핀");
        kokr.put("印度尼西亚", "인도네시아");
        kokr.put("印度", "인도");
        kokr.put("美国", "미국");
        kokr.put("加拿大", "캐나다");
        kokr.put("英国", "영국");
        kokr.put("法国", "프랑스");
        kokr.put("德国", "독일");
        kokr.put("西班牙", "스페인");
        kokr.put("意大利", "이탈리아");
        kokr.put("俄罗斯", "러시아");
        kokr.put("澳大利亚", "호주");
        kokr.put("新西兰", "뉴질랜드");
        kokr.put("巴西", "브라질");
        kokr.put("阿根廷", "아르헨티나");
        kokr.put("墨西哥", "멕시코");
        kokr.put("南非", "남아프리카 공화국");
        kokr.put("未知", "기타");
        HOMETOWN_TRANSLATIONS.put("kokr", kokr);
        HOMETOWN_TRANSLATIONS.put("KOKR", kokr);

        // 西班牙文
        Map<String, String> eses = new HashMap<>();
        eses.put("越南", "Vietnam");
        eses.put("泰国", "Tailandia");
        eses.put("中国", "China");
        eses.put("台湾", "Taiwán");
        eses.put("日本", "Japón");
        eses.put("香港", "Hong Kong");
        eses.put("韩国", "Corea del Sur");
        eses.put("新加坡", "Singapur");
        eses.put("马来西亚", "Malasia");
        eses.put("菲律宾", "Filipinas");
        eses.put("印度尼西亚", "Indonesia");
        eses.put("印度", "India");
        eses.put("美国", "Estados Unidos");
        eses.put("加拿大", "Canadá");
        eses.put("英国", "Reino Unido");
        eses.put("法国", "Francia");
        eses.put("德国", "Alemania");
        eses.put("西班牙", "España");
        eses.put("意大利", "Italia");
        eses.put("俄罗斯", "Rusia");
        eses.put("澳大利亚", "Australia");
        eses.put("新西兰", "Nueva Zelanda");
        eses.put("巴西", "Brasil");
        eses.put("阿根廷", "Argentina");
        eses.put("墨西哥", "México");
        eses.put("南非", "Sudáfrica");
        eses.put("未知", "Otro");
        HOMETOWN_TRANSLATIONS.put("eses", eses);
        HOMETOWN_TRANSLATIONS.put("ESES", eses);

        // 泰文
        Map<String, String> thth = new HashMap<>();
        thth.put("越南", "เวียดนาม");
        thth.put("泰国", "ประเทศไทย");
        thth.put("中国", "จีน");
        thth.put("台湾", "ไต้หวัน");
        thth.put("日本", "ญี่ปุ่น");
        thth.put("香港", "ฮ่องกง");
        thth.put("韩国", "เกาหลีใต้");
        thth.put("新加坡", "สิงคโปร์");
        thth.put("马来西亚", "มาเลเซีย");
        thth.put("菲律宾", "ฟิลิปปินส์");
        thth.put("印度尼西亚", "อินโดนีเซีย");
        thth.put("印度", "อินเดีย");
        thth.put("美国", "สหรัฐอเมริกา");
        thth.put("加拿大", "แคนาดา");
        thth.put("英国", "สหราชอาณาจักร");
        thth.put("法国", "ฝรั่งเศส");
        thth.put("德国", "เยอรมนี");
        thth.put("西班牙", "สเปน");
        thth.put("意大利", "อิตาลี");
        thth.put("俄罗斯", "รัสเซีย");
        thth.put("澳大利亚", "ออสเตรเลีย");
        thth.put("新西兰", "นิวซีแลนด์");
        thth.put("巴西", "บราซิล");
        thth.put("阿根廷", "อาร์เจนตินา");
        thth.put("墨西哥", "เม็กซิโก");
        thth.put("南非", "แอฟริกาใต้");
        thth.put("未知", "อื่น ๆ");
        HOMETOWN_TRANSLATIONS.put("thth", thth);
        HOMETOWN_TRANSLATIONS.put("THTH", thth);

        // 越南文
        Map<String, String> vivn = new HashMap<>();
        vivn.put("越南", "Việt Nam");
        vivn.put("泰国", "Thái Lan");
        vivn.put("中国", "Trung Quốc");
        vivn.put("台湾", "Đài Loan");
        vivn.put("日本", "Nhật Bản");
        vivn.put("香港", "Hồng Kông");
        vivn.put("韩国", "Hàn Quốc");
        vivn.put("新加坡", "Singapore");
        vivn.put("马来西亚", "Malaysia");
        vivn.put("菲律宾", "Philippines");
        vivn.put("印度尼西亚", "Indonesia");
        vivn.put("印度", "Ấn Độ");
        vivn.put("美国", "Mỹ");
        vivn.put("加拿大", "Canada");
        vivn.put("英国", "Vương quốc Anh");
        vivn.put("法国", "Pháp");
        vivn.put("德国", "Đức");
        vivn.put("西班牙", "Tây Ban Nha");
        vivn.put("意大利", "Ý");
        vivn.put("俄罗斯", "Nga");
        vivn.put("澳大利亚", "Úc");
        vivn.put("新西兰", "New Zealand");
        vivn.put("巴西", "Brazil");
        vivn.put("阿根廷", "Argentina");
        vivn.put("墨西哥", "Mexico");
        vivn.put("南非", "Nam Phi");
        vivn.put("未知", "Khác");
        HOMETOWN_TRANSLATIONS.put("vivn", vivn);
        HOMETOWN_TRANSLATIONS.put("VIVN", vivn);

        // 马来文
        Map<String, String> msmy = new HashMap<>();
        msmy.put("越南", "Vietnam");
        msmy.put("泰国", "Thailand");
        msmy.put("中国", "China");
        msmy.put("台湾", "Taiwan");
        msmy.put("日本", "Japan");
        msmy.put("香港", "Hong Kong");
        msmy.put("韩国", "South Korea");
        msmy.put("新加坡", "Singapore");
        msmy.put("马来西亚", "Malaysia");
        msmy.put("菲律宾", "Philippines");
        msmy.put("印度尼西亚", "Indonesia");
        msmy.put("印度", "India");
        msmy.put("美国", "United States");
        msmy.put("加拿大", "Canada");
        msmy.put("英国", "United Kingdom");
        msmy.put("法国", "France");
        msmy.put("德国", "Germany");
        msmy.put("西班牙", "Spain");
        msmy.put("意大利", "Italy");
        msmy.put("俄罗斯", "Russia");
        msmy.put("澳大利亚", "Australia");
        msmy.put("新西兰", "New Zealand");
        msmy.put("巴西", "Brazil");
        msmy.put("阿根廷", "Argentina");
        msmy.put("墨西哥", "Mexico");
        msmy.put("南非", "South Africa");
        msmy.put("未知", "Other");
        HOMETOWN_TRANSLATIONS.put("msmy", msmy);
        HOMETOWN_TRANSLATIONS.put("MSMY", msmy);
    }

    /**
     * 根据语言打包用户数据字段
     *
     * @param user 用户对象
     * @param lang 语言代码 (zhcn, zhtw, enus, jajp, kokr, eses, thth, vivn, msmy)
     * @return 处理后的用户对象
     */
    public static User packageUserDataFromFields(User user, String lang) {
        if (user == null) {
            return null;
        }

        // 如果没有指定语言或语言为空，返回原始用户
        if (!StringUtils.hasText(lang)) {
            return user;
        }

        // 标准化语言代码（转小写）
        String normalizedLang = lang.toLowerCase();

        // 处理用户名
        String localizedUsername = getLocalizedUsername(user, normalizedLang);
        if (StringUtils.hasText(localizedUsername)) {
            user.setUsername(localizedUsername);
        }

        // 处理介绍
        String localizedIntroduce = getLocalizedIntroduce(user, normalizedLang);
        if (StringUtils.hasText(localizedIntroduce)) {
            user.setIntroduce(localizedIntroduce);
        }

        // 处理家乡翻译
        if (StringUtils.hasText(user.getHometown())) {
            String translatedHometown = translateHometown(normalizedLang, user.getHometown());
            user.setHometown(translatedHometown);
        }

        return user;
    }

    /**
     * 获取本地化的用户名
     */
    private static String getLocalizedUsername(User user, String lang) {
        switch (lang) {
            case "zhtw":
                return StringUtils.hasText(user.getUsernameZHTW()) ? user.getUsernameZHTW() : user.getUsername();
            case "enus":
                return StringUtils.hasText(user.getUsernameENUS()) ? user.getUsernameENUS() : user.getUsername();
            case "jajp":
                return StringUtils.hasText(user.getUsernameJAJP()) ? user.getUsernameJAJP() : user.getUsername();
            case "kokr":
                return StringUtils.hasText(user.getUsernameKOKR()) ? user.getUsernameKOKR() : user.getUsername();
            case "eses":
                return StringUtils.hasText(user.getUsernameESES()) ? user.getUsernameESES() : user.getUsername();
            case "thth":
                return StringUtils.hasText(user.getUsernameTHTH()) ? user.getUsernameTHTH() : user.getUsername();
            case "vivn":
                return StringUtils.hasText(user.getUsernameVIVN()) ? user.getUsernameVIVN() : user.getUsername();
            case "msmy":
                return StringUtils.hasText(user.getUsernameMSMY()) ? user.getUsernameMSMY() : user.getUsername();
            default:
                return user.getUsername();
        }
    }

    /**
     * 获取本地化的介绍
     */
    private static String getLocalizedIntroduce(User user, String lang) {
        switch (lang) {
            case "zhtw":
                return StringUtils.hasText(user.getIntroduceZHTW()) ? user.getIntroduceZHTW() : user.getIntroduce();
            case "enus":
                return StringUtils.hasText(user.getIntroduceENUS()) ? user.getIntroduceENUS() : user.getIntroduce();
            case "jajp":
                return StringUtils.hasText(user.getIntroduceJAJP()) ? user.getIntroduceJAJP() : user.getIntroduce();
            case "kokr":
                return StringUtils.hasText(user.getIntroduceKOKR()) ? user.getIntroduceKOKR() : user.getIntroduce();
            case "eses":
                return StringUtils.hasText(user.getIntroduceESES()) ? user.getIntroduceESES() : user.getIntroduce();
            case "thth":
                return StringUtils.hasText(user.getIntroduceTHTH()) ? user.getIntroduceTHTH() : user.getIntroduce();
            case "vivn":
                return StringUtils.hasText(user.getIntroduceVIVN()) ? user.getIntroduceVIVN() : user.getIntroduce();
            case "msmy":
                return StringUtils.hasText(user.getIntroduceMSMY()) ? user.getIntroduceMSMY() : user.getIntroduce();
            default:
                return user.getIntroduce();
        }
    }

    /**
     * 翻译家乡名称
     *
     * @param lang 语言代码
     * @param hometown 家乡名称
     * @return 翻译后的家乡名称
     */
    public static String translateHometown(String lang, String hometown) {
        if (!StringUtils.hasText(hometown) || !StringUtils.hasText(lang)) {
            return hometown;
        }

        Map<String, String> translation = HOMETOWN_TRANSLATIONS.get(lang);
        if (translation == null) {
            // 如果找不到对应语言，使用英文作为默认
            translation = HOMETOWN_TRANSLATIONS.get("enus");
        }

        return translation.getOrDefault(hometown, hometown);
    }
}
