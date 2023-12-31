package com.ymgal.harvest.vndb;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ymgal.harvest.vndb.model.Release.Release;
import com.ymgal.harvest.vndb.model.Staff.Staff;
import com.ymgal.harvest.vndb.model.User.User;
import com.ymgal.harvest.vndb.model.VisualNovel.VisualNovel;
import com.ymgal.harvest.vndb.model.VndbFlags;
import com.ymgal.harvest.vndb.model.VndbResponse;
import com.ymgal.harvest.vndb.model.Character.Character;
import com.ymgal.harvest.vndb.model.Producer.Producer;

import java.util.stream.Collectors;

public class VndbGetMethod {

    private static String GetFullMethod(String method, String filters) {
        Integer vndbFlag = VndbUtils.getVndbFlag(method);
        String fields = VndbFlags.getDescs(vndbFlag).stream().collect(Collectors.joining(",", " ", " "));
        if ((method.equals(Constants.GetStaffCommand) || method.equals(Constants.GetCharacterCommand)) &&
                fields.contains(VndbFlags.VisualNovels.getDesc())) {
            fields = fields.replace(VndbFlags.VisualNovels.getDesc(), VndbFlags.VisualNovels.getDesc() + "s");
        }

        String cmd = method + fields + "(" + filters + ")";
        System.out.println("CMD:" + cmd);
        return cmd;
    }

    public static VndbResponse<VisualNovel> GetVisualNovel(String filters) {
        String cmd = GetFullMethod(Constants.GetVisualNovelCommand, filters);
        return VndbUtils.SendGetRequestInternalAsync(cmd, new TypeReference<VndbResponse<VisualNovel>>() {
        });
    }

    public static VndbResponse<Release> GetRelease(String filters) {
        String cmd = GetFullMethod(Constants.GetReleaseCommand, filters);
        return VndbUtils.SendGetRequestInternalAsync(cmd, new TypeReference<VndbResponse<Release>>() {
        });
    }

    public static VndbResponse<Producer> GetProducer(String filters) {
        String cmd = GetFullMethod(Constants.GetProducerCommand, filters);
        return VndbUtils.SendGetRequestInternalAsync(cmd, new TypeReference<VndbResponse<Producer>>() {
        });
    }

    public static VndbResponse<Character> GetCharacter(String filters) {
        String cmd = GetFullMethod(Constants.GetCharacterCommand, filters);
        return VndbUtils.SendGetRequestInternalAsync(cmd, new TypeReference<VndbResponse<Character>>() {
        });
    }

    public static VndbResponse<Staff> GetStaff(String filters) {
        String cmd = GetFullMethod(Constants.GetStaffCommand, filters);
        return VndbUtils.SendGetRequestInternalAsync(cmd, new TypeReference<VndbResponse<Staff>>() {
        });
    }

    public static VndbResponse<User> GetUser(String filters) {
        String cmd = GetFullMethod(Constants.GetUserCommand, filters);
        return VndbUtils.SendGetRequestInternalAsync(cmd, new TypeReference<VndbResponse<User>>() {
        });
    }
}
