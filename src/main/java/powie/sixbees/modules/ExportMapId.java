package powie.sixbees.modules;

import meteordevelopment.meteorclient.settings.FileSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import powie.sixbees.SixBees;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class ExportMapId extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<File> file = sgGeneral.add(new FileSetting.Builder()
        .name("file")
        .description("The file to write the map id to.")
        .filter("txt")
        .build()
    );

    private final Setting<Keybind> writeKey = sgGeneral.add(new KeybindSetting.Builder()
        .name("write-keybind")
        .description("The key to write the map id to the file")
        .action(this::handleWriteKey)
        .build()
    );

    private final Setting<Keybind> WriteNewlineKey = sgGeneral.add(new KeybindSetting.Builder()
        .name("write-blank-line-keybind")
        .description("The key to write a blank line to the file")
        .action(this::handleWriteNewlineKey)
        .build()
    );

    public ExportMapId() {
        super(SixBees.CATEGORY, "export-map-id", "Export map ID you're looking at to file");
    }

    private void handleWriteKey() {
        if (!Files.exists(file.get().toPath())) {
            error("No file selected");
            return;
        }
        if (!Utils.canUpdate() || mc.hitResult == null) return;
        if (mc.hitResult.getType() == HitResult.Type.ENTITY
            && ((EntityHitResult) mc.hitResult).getEntity() instanceof ItemFrame frame
            && frame.hasFramedMap()) {
            String id = String.valueOf(frame.getFramedMapId(frame.getItem()).id());
            appendLine(file.get(), id);
            info(id + " added");
        }
    }

    private void handleWriteNewlineKey() {
        if (!Files.exists(file.get().toPath())) {
            error("No file selected");
            return;
        }
        if (!Utils.canUpdate()) return;
        appendBlankLine(file.get());
        info("blank line added");
    }

    private void appendLine(File file, String line) {
        try {
            Files.writeString(
                file.toPath(),
                line + System.lineSeparator(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            error("Failed to write to file: " + e.getMessage());
        }
    }

    private void appendBlankLine(File file) {
        appendLine(file, "");
    }

}
