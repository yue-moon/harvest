package com.ymgal.harvest;

import com.ymgal.harvest.model.archive.OrgArchive;
import com.ymgal.harvest.model.archive.PersonArchive;
import com.ymgal.harvest.vndb.VndbGetMethodByHttp;
import com.ymgal.harvest.vndb.helper.DateTimeHelper;
import com.ymgal.harvest.vndb.helper.TcpHelper;
import com.ymgal.harvest.vndb.model.Release.Release;
import com.ymgal.harvest.vndb.model.Staff.Staff;
import com.ymgal.harvest.vndb.modelhttp.VndbFilter;
import com.ymgal.harvest.vndb.modelhttp.vo.Vn;
import com.ymgal.harvest.vndb.modelhttp.vo.common.Exlink;
import com.ymgal.harvest.vndb.VndbGetMethod;
import com.ymgal.harvest.vndb.filter.VndbFilters;
import com.ymgal.harvest.model.ExtensionName;
import com.ymgal.harvest.model.Website;
import com.ymgal.harvest.model.archive.CharacterArchive;
import com.ymgal.harvest.model.archive.GameArchive;
import com.ymgal.harvest.vndb.model.Character.Character;
import com.ymgal.harvest.vndb.model.Character.VoiceActorMetadata;
import com.ymgal.harvest.vndb.model.Producer.Producer;
import com.ymgal.harvest.vndb.model.VisualNovel.VisualNovel;
import com.ymgal.harvest.vndb.model.VndbResponse;
import com.ymgal.harvest.vndb.modelhttp.enums.FilterName;
import com.ymgal.harvest.vndb.modelhttp.enums.FilterOperator;

import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

public class VndbHarvest extends Harvest {

    private static final String PREFIX = "https://vndb.org/v";

    public VndbHarvest(String gameUrl) {
        super(gameUrl);
    }

    @Override
    protected void validateUrl(String gameUrl) {
        if (gameUrl.startsWith(PREFIX)) return;
        throw new IllegalArgumentException("VNDB address parsing error: " + gameUrl);
    }


    @Override
    protected HarvestResult exec(String gameUrl, InetSocketAddress proxy) {

        Integer vnid = Integer.parseInt(gameUrl.split(PREFIX)[1]);
        TcpHelper.Login();
        GameArchive gameAcrhive = getGameAcrhive(vnid);
        OrgArchive orgArchive = getOrgArchive(gameAcrhive.getDeveloper());
        List<PersonArchive> personArchiveList = getPersonArchiveList(gameAcrhive.getStaff().stream().map(x -> x.getSid()).toArray(x -> new Integer[x]));
        List<CharacterArchive> characterArchiveList = getCharacterArchiveList(vnid);

        HarvestResult harvestResult = HarvestResult.ok(
                gameAcrhive, orgArchive, personArchiveList, characterArchiveList
        );
        TcpHelper.Loginout();

        return harvestResult;
    }

    public static GameArchive getGameAcrhive(Integer vnid) {

        // HTTP查询数据
        VndbFilter vndbFilter = new VndbFilter(FilterName.ID.getFilterName(), FilterOperator.EQ.getOperator(), vnid + "");
        VndbResponse<Vn> responseBody = VndbGetMethodByHttp.GetVisualNovel(vndbFilter);
        if (responseBody == null || responseBody.getItems() == null || responseBody.getItems().size() == 0) {
            return null;
        }

        Vn vn = responseBody.getItems().get(0);

        GameArchive archive = new GameArchive();
        // ID
        archive.setVndbId(Integer.parseInt(vn.getId().replace("v", "")));
        // 标题
        archive.setName(vn.getTitle());

        // 是否有汉化
        if (vn.getLanguages() != null) {
            archive.setHaveChinese(vn.getLanguages().stream().anyMatch(x -> "zh-Hans".equals(x) | "zh-Hant".equals(x)));
        }

        // 获取Links
        List<Exlink> linksByHtml = VndbGetMethodByHttp.getLinksByHtml(PREFIX + vnid);
        List<Website> websites = linksByHtml.stream().map(x -> {
            return new Website(x.getName(), x.getUrl());
        }).collect(Collectors.toList());
        archive.setWebsite(websites);

        // 图片
        archive.setMainImg(vn.getImage().getUrl());
        // 游戏类型
        // 没有类型描述字段
        archive.setTypeDesc("");
        // 开发商
        // 有多家开发商，取第一家
        if (vn.getDevelopers() != null) {
            archive.setDeveloper(Integer.parseInt(vn.getDevelopers().get(0).getId().replace("p", "")));
        }

        // 发售日期
        archive.setReleaseDate(LocalDate.parse(vn.getReleased()));

        // 扩展名
        archive.setExtensionName(new ArrayList<>());
        if (vn.getTitles() != null) {
            List<ExtensionName> extensionNames = vn.getTitles().stream()
                    .filter(x -> x.getTitle() != null && x.getTitle().trim().length() > 0)
                    .map(x -> new ExtensionName(x.getTitle(), x.getLang()))
                    .collect(toList());
            archive.setExtensionName(extensionNames);
        }

        //简介
        archive.setIntroduction(vn.getDescription() == null ? "" : vn.getDescription());


        // 角色
        VndbResponse<Character> character_tcp = VndbGetMethod.GetCharacter(VndbFilters.VisualNovel.Equals(vnid).toString());
        if (character_tcp.getItems() != null) {
            List<GameArchive.Characters> characters = character_tcp.getItems().stream().map(x -> {
                return new GameArchive().new Characters(
                        x.getId(),
                        x.getVoiced().stream().filter(v -> Objects.equals(v.getVid(), vnid))
                                .findFirst().map(VoiceActorMetadata::getId).orElse(0),
                        x.getVns().stream().filter(p -> Objects.equals(p[0], vnid)).findFirst().map(m -> (String) m[3]).get().equals("main") ? 1 : 0
                );
            }).collect(Collectors.toList());
            archive.setCharacters(characters);
        }


        // 发售信息， 可能发售了多个平台  国家没有只有语言  LocalDate格式化的问题
        VndbResponse<Release> release_tcp = VndbGetMethod.GetRelease(VndbFilters.VisualNovel.Equals(vnid).toString());
        if (release_tcp.getItems() != null) {
            List<GameArchive.Release> releases = release_tcp.getItems().stream().map(x -> {
                return new GameArchive().new Release(x.getTitle(), x.getWebsite(),
                        x.getPlatforms().stream().collect(Collectors.joining(",")),
                        // TODO 时间格式化问题  LocalDate.parse(x.getReleased()),
                        DateTimeHelper.DateFormatConvert(x.getReleased()),
                        x.getLanguages().stream().collect(Collectors.joining(",")),
                        String.valueOf(x.getMinage()));
            }).collect(Collectors.toList());
            archive.setReleases(releases);

            //是否受限制
            archive.setRestricted(release_tcp.getItems().stream().anyMatch(x -> x.getMinage() > 0));
        }

        //Staff
        VndbResponse<VisualNovel> visualNovelVndbResponse = VndbGetMethod.GetVisualNovel(VndbFilters.Id.Equals(vnid).toString());
        if (visualNovelVndbResponse.getItems() != null) {
            VisualNovel vn_tcp = visualNovelVndbResponse.getItems().get(0);
            List<GameArchive.Staff> staff = vn_tcp.getStaff().stream().map(x -> {
                return new GameArchive().new Staff(x.getSid(), x.getName(), x.getNote(), x.getRole());
            }).collect(Collectors.toList());
            archive.setStaff(staff);
        }

        return archive;
    }

    public static OrgArchive getOrgArchive(Integer orgid) {

        OrgArchive orgArchive = new OrgArchive();

        VndbResponse<Producer> ProducerVndbResponse = VndbGetMethod.GetProducer(VndbFilters.Id.Equals(orgid).toString());
        if (ProducerVndbResponse == null || ProducerVndbResponse.getItems() == null || ProducerVndbResponse.getItems().size() == 0) {
            return null;
        }

        Producer producer = ProducerVndbResponse.getItems().get(0);

        orgArchive.setVndbPid(orgid);
        orgArchive.setOrgName(producer.getName());
        orgArchive.setCountry(producer.getLanguage());

        orgArchive.setIntroduction(producer.getDescription() == null ? "" : producer.getDescription());


        // 网站
        if (producer.getLinks() != null) {
            String title = producer.getLinks().getWikidata() == null ? producer.getLinks().getWikipedia() : producer.getLinks().getWikidata();
            orgArchive.setWebsite(new ArrayList<Website>() {
                {
                    if (title != null && producer.getLinks() != null && producer.getLinks().getHomepage() != null) {
                        this.add(new Website(title,
                                producer.getLinks().getHomepage()));
                    }
                }
            });
        }

        //只有别名
        orgArchive.setExtensionNames(Collections.emptyList());
        if (producer.getAliases() != null && producer.getAliases().trim().length() > 0) {
            orgArchive.setExtensionNames(Collections.singletonList(new ExtensionName(producer.getAliases())));
        }

        return orgArchive;
    }

    public static List<PersonArchive> getPersonArchiveList(Integer[] staffIds) {
        VndbResponse<Staff> StaffVndbResponse = VndbGetMethod.GetStaff(VndbFilters.Id.Equals(staffIds).toString());
        if (StaffVndbResponse == null || StaffVndbResponse.getItems() == null || StaffVndbResponse.getItems().size() == 0) {
            return null;
        }
        List<Staff> staffList = StaffVndbResponse.getItems();
        List<PersonArchive> personArchiveList = new ArrayList<>();
        for (Staff staff : staffList) {
            PersonArchive personArchive = new PersonArchive();
            personArchive.setVndbSid(staff.getId());
            personArchive.setName(staff.getName());
            personArchive.setExtensionNames(new ArrayList<>());

            if (staff.getAliases() != null) {
                List<ExtensionName> names = staff.getAliases()
                        .stream()
                        .map(a -> (String) (a[2] == null ? a[1] : a[2]))
                        .filter(name -> name != null && name.trim().length() > 0)
                        .map(ExtensionName::new)
                        .collect(toList());
                personArchive.setExtensionNames(names);
            }

            personArchive.setIntroduction(staff.getDescription() == null ? "" : staff.getDescription());

            personArchive.setCountry(staff.getLanguage());
            if (staff.getLinks() != null) {
                String title = staff.getLinks().getWikidata() == null ? staff.getLinks().getWikipedia() : staff.getLinks().getWikidata();
                personArchive.setWebsite(new ArrayList<Website>() {{
                    if (title != null && staff.getLinks() != null && staff.getLinks().getHomepage() != null) {
                        this.add(new Website(title, staff.getLinks().getHomepage()));
                    }
                }});
            }
            //默认0未知 1男 2女
            personArchive.setGender(0);
            if (staff.getGender() != null) {
                if (staff.getGender().equals("m")) {
                    personArchive.setGender(1);
                } else if (staff.getGender().equals("f")) {
                    personArchive.setGender(2);
                }
            }
            personArchiveList.add(personArchive);
        }

        return personArchiveList;
    }

    public static List<CharacterArchive> getCharacterArchiveList(Integer vnid) {
        VndbResponse<Character> CharacterVndbResponse = VndbGetMethod.GetCharacter(VndbFilters.VisualNovel.Equals(vnid).toString());
        if (CharacterVndbResponse == null || CharacterVndbResponse.getItems() == null || CharacterVndbResponse.getItems().size() == 0) {
            return null;
        }
        List<Character> characterList = CharacterVndbResponse.getItems();
        List<CharacterArchive> characterArchiveList = new ArrayList<>();
        for (Character character : characterList) {
            CharacterArchive characterArchive = new CharacterArchive();
            characterArchive.setVndbCid(character.getId());
            characterArchive.setName(character.getName());

            // 扩展名
            characterArchive.setExtensionNames(new ArrayList<>());
            if (character.getAliases() != null) {
                String[] aliaseList = character.getAliases().split("\n");

                List<ExtensionName> names = Arrays.stream(aliaseList)
                        .filter(x -> x != null && x.trim().length() > 0)
                        .map(x -> new ExtensionName(x))
                        .collect(toList());
                characterArchive.setExtensionNames(names);
            }

            characterArchive.setIntroduction(character.getDescription() == null ? "" : character.getDescription());
            if (character.getBirthday() != null && character.getBirthday().get(0) != null && character.getBirthday().get(1) != null) {
                characterArchive.setBirthday(LocalDate.of(3000, character.getBirthday().get(1), character.getBirthday().get(0)));
            }
            characterArchive.setMainImg(character.getImage());

            //默认0未知 1男 2女
            characterArchive.setGender(0);
            if (character.getGender() != null) {
                if (character.getGender().equals("m")) {
                    characterArchive.setGender(1);
                } else if (character.getGender().equals("f")) {
                    characterArchive.setGender(2);
                }
            }
            characterArchiveList.add(characterArchive);
        }
        return characterArchiveList;
    }

}
