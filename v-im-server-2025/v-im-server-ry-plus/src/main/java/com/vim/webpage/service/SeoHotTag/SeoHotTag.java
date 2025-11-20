package com.vim.webpage.service.SeoHotTag;

/*
    @author fres
    @description: 获取SEO热门标签服务
 */
public class SeoHotTag {
    public static String getHotTags(String lang) {
        // 处理语言代码：转小写、移除"-"、空值默认为"zhcn"
        lang = (lang == null || lang.trim().isEmpty())
                ? "zhcn"
                : lang.toLowerCase().replace("-", "");
        return getSEOkeywords(lang);
    }

    public static String getSEOkeywords(String lang) {
        switch (lang) {
            case "zhcn":
                return "乱伦 | UU猫 | 国产 | 日本 | 女优 | yuumeilyn | 动漫 | 台湾 | 朱古力 | 女性向 | " +
                       "无码 | 韩国 | coser视频 | KittyxKum | 疯 | 三上悠亚 | 中国 | 熟女 | 自拍 | 辛尤里 | " +
                       "越南 | jk视频 | 母子乱伦 | 福利姬 | 剧情 | 旗袍 | 泰國 | 午夜 | 波多野結衣 | 秀人网 | " +
                       "中文 | 搭讪 | 大胸 | 巨乳 | 动漫 | 朱 | 中文字幕 | 帝王 | 酷愛 | 重口味";
            case "zhtw":
                return "亂倫 | UU猫 | 國產 | 日本 | 女優 | yuumeilyn | 動漫 | 台灣 | 朱古力 | 女性向 | " +
                       "無碼 | 韓國 | coser视频 | KittyxKum | 疯 | 三上悠亞 | 中國 | 熟女 | 自拍 | 辛尤里 | " +
                       "越南 | jk视频 | 母子亂倫 | 福利姬 | 劇情 | 旗袍 | 泰國 | 午夜 | 波多野結衣 | 秀人网 | " +
                       "中文 | 搭讪 | 大胸 | 巨乳 | 动漫 | 朱 | 中文字幕 | 帝王 | 酷愛 | 重口味";
            case "jajp":
                return "gif | tubing | 巨乳 | オンライン​ | ギャル​ | サンプル​ | 瀬戸環奈 | 女優無修正​ | 朱古力 | 女性向け​ | " +
                       "無碼 | 画像​ | デビュー​ | 瀬戸環奈 | オンライン | 爆乳 | ショートカット​| ランキング​ | xsz | ぽっちゃり | " +
                       "越南 | 剛毛 | みす | 三上悠亜 | 研究所​ | オンライン​ | ギャル​ | ランキング​ | かわいい​ | かわいい | " +
                       "イケメン | マッサージ | ナチュラルハイ​ | えろ​ | 飯島愛 | ロケット​| あんあん | 海外 | ショートカット | エロ​";
            case "kokr":
                return "쏘걸​ | 일본 | 핑크​ | 사이트 | 한국 | 추천 | 검색 | 불륜야동​ | 불륜 | 동인지​ | " +
                       "배우​ | 바람피는 | 설리 | 신태일 | 고딩 | 국산 | 모자이크없음| 스튜어디스 | 학생 | 한국 | " +
                       "간호원 | 부산| 딸램 | 노모 | 존예녀 | 화장실녀 | 근친상간 | 발기 | 부녀가 | 신태일 | " +
                       "품번​ | 스트리밍​ | 쏘​​ | 국산 | 오구라유나 | 보기​ | 서양 | 모아​ | 보는곳​| japan";
            case "eses":
                return "foro | pelis | gay | omegle | películas | japones | erome | mature | incesto | onlyfans | " +
                       "cecilia sopeña |camara oculta| comic pornos 3d | jameliz | manga | gay | amateur español| comic | casting | jameliz | " +
                       "españolas gratis | gratis | trío | china | mature | hard core | hammter | abuelita | árabes | italiano | " +
                       "peliculas | cecilia sopeña | peli | sub | hegemon | erótico | soofilia | a nal | abuelita | actrices";
            case "enus":
                return "foro | pelis| gay| omegle | películas | japones | erome | mature | incesto| onlyfans | " +
                       "incesto | sophie rain | cnc  | fortnite  | gay furry  | horse  | jameliz | furry comics | gay comics | marvel rivals | " +
                       "md | roblox| camilla araujo | hypno | spice | ice spice | jameliz | japanese | crossplay | ari kytsya | " +
                       "dark | dog | jenna ortega  | katiana kay | morgpie | rape | amouranth | chinese | breckie hill| japan";
            case "thth":
                return "สาว ไทย​| ไทย| subthai | omegle | películas | th | gal gadot | แตกใน | สาว สวย| อวบ | " +
                       "superheroine | ultraman | สาว อวบ | jav | ไทย | uncen | china | chinese | thaisub | ดารา  | " +
                       "หนัง ญี่ปุ่น | พาก ไทย| สาว ไทย | สาว อวบ | แตก ใน | สาว สวย | japan | ญี่ปุ่น | love | นมใหญ่ | " +
                       "xxx สาว ไทย​ | สาว ไทย xxx | โป๊ สาว ไทย​ | porn สาว ไทย​ | ภาพ โป๊​ | การ์ตูน | สาว ไทย​ | เกาหลี | ญี่ปุ่น| ผู้หญิง";
            case "vivn":
                return "luân | phim sex loạn luân không| truyện loạn | nhâm gia luân | jav loạn | Mẹ và con trai của loạn luân | phim sex loạn luân việt nam | mature | incesto| onlyfans | " +
                       "cecilia sopeña | truyện loạn | phim xxx | xxx ko che | trung quoc | nhat ban | châu âu| trung | china | hàn quốc | " +
                       "nhật | nhật bản | khong che | dan choi | ảnh | chinese | trung quốc | phim sẽ | phim sex mỹ | loan luan | " +
                       "nhật bản | truyện tranh | sẽ gai xinh | vũ luân | sex loạn luân việt nam | hentai loạn luân | thái lan | hiep dam | cấp 3 | châu á​";
            case "msmy":
                return " jepun | lucah| novel | filem lucah | kisah lucah | komik | sumbang mahram | Sumbang mahram ibu-anak | bapa-anak perempuan | onlyfans | " +
                       "cerita | awek melayu | telegram melayu | cerita lucah isteri | cerita lucah makcik | filem seks | cerita seks melayu | cerita seks| cerita seks cikgu | korea selatan | " +
                       "China lucah| melayu tandas curi rakam | dorm cam Malaysia | seks pelajar kolej | muslim girl porn Malaysia | melayu tudung sex | malaysian porn |masakan stim | awek melayu stim | pelajar kolej lucah | " +
                       "lucah bawah umur | skandal pejabat | lucah dalam kereta | awek main dalam tandas | tudung sex | tudung seks | Malay blowjob | Malay couple hotel | cikgu melayu seks| main dalam bilik sewa";
            default:
                return "";
        }
    }

}
