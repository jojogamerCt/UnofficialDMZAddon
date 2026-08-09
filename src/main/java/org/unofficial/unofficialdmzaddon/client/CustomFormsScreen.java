package org.unofficial.unofficialdmzaddon.client;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.ColorSlider;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.util.BaseMenuScreen;
import com.dragonminez.client.render.effects.AuraRenderer;
import com.dragonminez.client.render.layer.DMZSkinLayer;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;
import org.unofficial.unofficialdmzaddon.UnofficialDMZConfig;
import org.unofficial.unofficialdmzaddon.dmz.CustomFormDefinition;
import org.unofficial.unofficialdmzaddon.network.AddonNetwork;
import org.unofficial.unofficialdmzaddon.network.CustomFormSaveResultS2C;
import org.unofficial.unofficialdmzaddon.network.DeleteCustomFormC2S;
import org.unofficial.unofficialdmzaddon.network.SaveCustomFormC2S;
import org.unofficial.unofficialdmzaddon.network.SelectCustomFormC2S;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class CustomFormsScreen extends BaseMenuScreen {
    private static final ResourceLocation MENU_BIG = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menubig.png");
    private static final ResourceLocation BUTTONS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/characterbuttons.png");
    private static final String PREVIEW_GROUP = "unofficialdmzaddon_custom_form_preview";
    private static final String PREVIEW_FORM = "preview";
    private static final String[] HAIR_TYPES = {"base", "ssj", "ssj2", "ssj3"};
    private static final int PAGE_COUNT = 5;

    private boolean editing;
    private int page;
    private int selectedIndex = -1;
    private String editingId = "";
    private String formName = "Custom Form";
    private int hairIndex;
    private String hairColor = "#FFFFFF";
    private String eye1Color = "#00FFFF";
    private String eye2Color = "#00FFFF";
    private String auraColor = "#FFFFFF";
    private String bodyColor = "#FFFFFF";
    private String tailColor = "#572117";
    private double multiplier = 1.5D;
    private double drain = 0.05D;
    private String colorTarget = "hair";
    private ColorSlider hueSlider;
    private ColorSlider saturationSlider;
    private ColorSlider valueSlider;
    private EditBox nameField;
    private final Map<String, TexturedTextButton> colorButtons = new LinkedHashMap<>();
    private boolean updatingColor;
    private boolean pendingSave;
    private String resultKey = "";
    private int resultCost;
    private float playerRotation = 180.0F;
    private float playerPitch = 8.0F;
    private boolean draggingModel;
    private double lastMouseX;
    private double lastMouseY;

    public CustomFormsScreen() { super(Component.translatable("gui.unofficialdmzaddon.custom_forms")); }

    @Override
    protected int getMinGuiWidth() { return 520; }

    @Override
    protected void init() {
        super.init();
        rebuildWidgets();
    }

    @Override
    protected void rebuildWidgets() {
        clearWidgets();
        colorButtons.clear();
        initNavigationButtons();
        if (editing) initEditorWidgets();
        else initListWidgets();
    }

    private void initListWidgets() {
        int top = panelTop();
        int right = getUiWidth() - 153;
        addRenderableWidget(textButton(right + 34, top + 92, tr("gui.unofficialdmzaddon.custom_forms.new"), this::newForm));
        addRenderableWidget(textButton(right + 34, top + 115, tr("gui.unofficialdmzaddon.custom_forms.edit"), this::editSelected));
        addRenderableWidget(textButton(right + 34, top + 138, tr("gui.unofficialdmzaddon.custom_forms.select"), this::select));
        addRenderableWidget(textButton(right + 34, top + 161, tr("gui.unofficialdmzaddon.custom_forms.delete"), this::delete));
    }

    private void initEditorWidgets() {
        int left = 12;
        int top = panelTop();
        if (page == 0) {
            nameField = new EditBox(font, left + 25, top + 72, 91, 14, tr("gui.unofficialdmzaddon.custom_forms.name"));
            nameField.setMaxLength(24);
            nameField.setValue(formName);
            nameField.setResponder(value -> formName = value);
            addRenderableWidget(nameField);
        } else if (page == 1) {
            colorTarget = "hair";
            addRenderableWidget(arrow(left + 20, top + 58, true, () -> changeHair(-1)));
            addRenderableWidget(arrow(left + 111, top + 58, false, () -> changeHair(1)));
            addRenderableWidget(colorButton(left + 34, top + 89, "hair"));
            initColorSliders(left + 21, top + 126);
        } else if (page == 2) {
            if (!colorTarget.equals("eye1") && !colorTarget.equals("eye2") && !colorTarget.equals("body")) colorTarget = "eye1";
            addRenderableWidget(colorButton(left + 34, top + 47, "eye1"));
            addRenderableWidget(colorButton(left + 34, top + 72, "eye2"));
            addRenderableWidget(colorButton(left + 34, top + 97, "body"));
            initColorSliders(left + 21, top + 137);
        } else if (page == 3) {
            if (!colorTarget.equals("aura") && !(isSaiyan() && colorTarget.equals("tail"))) colorTarget = "aura";
            addRenderableWidget(colorButton(left + 34, top + 55, "aura"));
            if (isSaiyan()) addRenderableWidget(colorButton(left + 34, top + 84, "tail"));
            initColorSliders(left + 21, top + 126);
        } else {
            addRenderableWidget(arrow(left + 20, top + 54, true,
                    () -> { multiplier = Mth.clamp(multiplier - 0.1D, 1.0D, UnofficialDMZConfig.CUSTOM_FORMS_MAX_MULTIPLIER.get()); updateSaveButton(); }));
            addRenderableWidget(arrow(left + 111, top + 54, false,
                    () -> { multiplier = Mth.clamp(multiplier + 0.1D, 1.0D, UnofficialDMZConfig.CUSTOM_FORMS_MAX_MULTIPLIER.get()); updateSaveButton(); }));
            addRenderableWidget(arrow(left + 20, top + 91, true,
                    () -> { drain = Mth.clamp(drain - 0.01D, 0.0D, UnofficialDMZConfig.CUSTOM_FORMS_MAX_ENERGY_DRAIN.get()); updateSaveButton(); }));
            addRenderableWidget(arrow(left + 111, top + 91, false,
                    () -> { drain = Mth.clamp(drain + 0.01D, 0.0D, UnofficialDMZConfig.CUSTOM_FORMS_MAX_ENERGY_DRAIN.get()); updateSaveButton(); }));
        }

        int center = getUiWidth() / 2;
        addRenderableWidget(textButton(center - 78, top + 188, tr("gui.dragonminez.customization.back"), this::previousPage));
        Component next = page == PAGE_COUNT - 1
                ? tr("gui.unofficialdmzaddon.custom_forms.save")
                : tr("gui.dragonminez.customization.next");
        TexturedTextButton action = textButton(center + 4, top + 188, next,
                page == PAGE_COUNT - 1 ? this::save : this::nextPage);
        action.active = page != PAGE_COUNT - 1 || (!pendingSave && availableTp() >= pendingCost());
        addRenderableWidget(action);
    }

    private void updateSaveButton() { rebuildWidgets(); }

    private void initColorSliders(int x, int y) {
        float[] hsv = ColorUtils.hexToHsv(targetColor());
        hueSlider = colorSlider(x, y, 0, 360, Math.round(hsv[0]), "Hue");
        saturationSlider = colorSlider(x, y + 15, 100, 0, Math.round(hsv[1]), "Saturation");
        valueSlider = colorSlider(x, y + 30, 100, 0, Math.round(hsv[2]), "Value");
        hueSlider.setCurrentHue(hsv[0]);
        saturationSlider.setCurrentHue(hsv[0]);
        valueSlider.setCurrentHue(hsv[0]);
        valueSlider.setCurrentSaturation(hsv[1]);
        addRenderableWidget(hueSlider);
        addRenderableWidget(saturationSlider);
        addRenderableWidget(valueSlider);
    }

    private ColorSlider colorSlider(int x, int y, int min, int max, int value, String label) {
        return new ColorSlider.Builder().position(x, y).size(100, 10).range(min, max).value(value)
                .message(Component.literal(label)).onValueChange(ignored -> updateColorFromSliders()).build();
    }

    private void updateColorFromSliders() {
        if (updatingColor || hueSlider == null || saturationSlider == null || valueSlider == null) return;
        float hue = hueSlider.getValue();
        float saturation = saturationSlider.getValue();
        float value = valueSlider.getValue();
        saturationSlider.setCurrentHue(hue);
        valueSlider.setCurrentHue(hue);
        valueSlider.setCurrentSaturation(saturation);
        String color = ColorUtils.hsvToHex(hue, saturation, value);
        setTargetColor(color);
        TexturedTextButton button = colorButtons.get(colorTarget);
        if (button != null) button.setBackgroundColor(ColorUtils.hexToInt(color));
    }

    private TexturedTextButton colorButton(int x, int y, String target) {
        int color = ColorUtils.hexToInt(colorFor(target));
        TexturedTextButton button = new TexturedTextButton.Builder().position(x, y).size(74, 20).texture(BUTTONS)
                .textureCoords(0, 28, 0, 48).textureSize(74, 20)
                .backgroundColor(color).message(tr("gui.unofficialdmzaddon.custom_forms.color." + target))
                .onPress(pressed -> { colorTarget = target; rebuildWidgets(); }).build();
        colorButtons.put(target, button);
        return button;
    }

    private CustomTextureButton arrow(int x, int y, boolean left, Runnable action) {
        return new CustomTextureButton.Builder().position(x, y).size(10, 15).texture(BUTTONS)
                .textureCoords(left ? 32 : 20, 0, left ? 32 : 20, 14).textureSize(8, 14)
                .onPress(button -> action.run()).build();
    }

    private TexturedTextButton textButton(int x, int y, Component message, Runnable action) {
        return new TexturedTextButton.Builder().position(x, y).size(74, 20).texture(BUTTONS)
                .textureCoords(0, 28, 0, 48).textureSize(74, 20).message(message)
                .onPress(button -> action.run()).build();
    }

    private void newForm() {
        resetDraft();
        editing = true;
        page = 0;
        rebuildWidgets();
    }

    private void editSelected() {
        CustomFormDefinition selected = selected();
        if (selected == null) return;
        loadDraft(selected);
        editing = true;
        page = 0;
        rebuildWidgets();
    }

    private void nextPage() { if (page < PAGE_COUNT - 1) { page++; rebuildWidgets(); } }

    private void previousPage() {
        if (page > 0) { page--; rebuildWidgets(); }
        else { editing = false; resultKey = ""; rebuildWidgets(); }
    }

    private void changeHair(int delta) {
        hairIndex = Math.floorMod(hairIndex + delta, HAIR_TYPES.length);
        rebuildWidgets();
    }

    private void save() {
        if (pendingSave) return;
        if (editingId.isEmpty()) editingId = "form_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        pendingSave = true;
        resultKey = "gui.unofficialdmzaddon.custom_forms.saving";
        resultCost = pendingCost();
        AddonNetwork.sendToServer(new SaveCustomFormC2S(draft()));
        rebuildWidgets();
    }

    private void select() {
        CustomFormDefinition selected = selected();
        if (selected != null) AddonNetwork.sendToServer(new SelectCustomFormC2S(selected.id()));
    }

    private void delete() {
        CustomFormDefinition selected = selected();
        if (selected == null) return;
        AddonNetwork.sendToServer(new DeleteCustomFormC2S(selected.id()));
        selectedIndex = -1;
        resetDraft();
        rebuildWidgets();
    }

    private CustomFormDefinition selected() {
        List<CustomFormDefinition> forms = forms();
        return selectedIndex >= 0 && selectedIndex < forms.size() ? forms.get(selectedIndex) : null;
    }

    private CustomFormDefinition draft() {
        return new CustomFormDefinition(editingId, formName, race(), HAIR_TYPES[hairIndex], hairColor,
                eye1Color, eye2Color, auraColor, bodyColor, tailColor, multiplier, drain);
    }

    private void loadDraft(CustomFormDefinition form) {
        editingId = form.id();
        formName = form.name();
        hairIndex = 0;
        for (int i = 0; i < HAIR_TYPES.length; i++) if (HAIR_TYPES[i].equalsIgnoreCase(form.hairType())) hairIndex = i;
        hairColor = form.hairColor();
        eye1Color = form.eye1Color();
        eye2Color = form.eye2Color();
        auraColor = form.auraColor();
        bodyColor = form.bodyColor();
        tailColor = form.tailColor();
        multiplier = form.multiplier();
        drain = form.energyDrain();
        colorTarget = "hair";
        resultKey = "";
    }

    private void resetDraft() {
        editingId = "";
        formName = "Custom Form";
        hairIndex = 0;
        hairColor = "#FFFFFF";
        eye1Color = "#00FFFF";
        eye2Color = "#00FFFF";
        auraColor = "#FFFFFF";
        bodyColor = "#FFFFFF";
        tailColor = "#572117";
        multiplier = 1.5D;
        drain = 0.05D;
        colorTarget = "hair";
        resultKey = "";
        pendingSave = false;
    }

    private int pendingCost() {
        int previous = 0;
        if (!editingId.isEmpty()) {
            for (CustomFormDefinition form : forms()) if (form.id().equals(editingId)) previous = form.creationCost();
        }
        return Math.max(0, draft().creationCost() - previous);
    }

    private int availableTp() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0;
        return StatsProvider.get(StatsCapability.INSTANCE, mc.player)
                .map(data -> Math.max(0, (int) data.getResources().getTrainingPoints())).orElse(0);
    }

    private String race() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return "";
        return StatsProvider.get(StatsCapability.INSTANCE, mc.player)
                .map(data -> data.getCharacter().getRaceName()).orElse("");
    }

    private boolean isSaiyan() { return "saiyan".equalsIgnoreCase(race()); }

    private List<CustomFormDefinition> forms() { return CustomFormsClientState.forRace(race()); }

    private String colorFor(String target) {
        return switch (target) {
            case "eye1" -> eye1Color;
            case "eye2" -> eye2Color;
            case "body" -> bodyColor;
            case "aura" -> auraColor;
            case "tail" -> tailColor;
            default -> hairColor;
        };
    }

    private String targetColor() { return colorFor(colorTarget); }

    private void setTargetColor(String color) {
        switch (colorTarget) {
            case "eye1" -> eye1Color = color;
            case "eye2" -> eye2Color = color;
            case "body" -> bodyColor = color;
            case "aura" -> auraColor = color;
            case "tail" -> tailColor = color;
            default -> hairColor = color;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (selectedIndex >= forms().size()) selectedIndex = -1;
        CustomFormSaveResultS2C result = CustomFormsClientState.consumeSaveResult();
        if (result != null) {
            pendingSave = false;
            resultCost = result.chargedTp();
            if (result.success()) {
                editingId = result.id();
                resultKey = "gui.unofficialdmzaddon.custom_forms.saved";
                List<CustomFormDefinition> current = forms();
                for (int i = 0; i < current.size(); i++) if (current.get(i).id().equals(editingId)) selectedIndex = i;
                editing = false;
            } else resultKey = "gui.unofficialdmzaddon.custom_forms.error." + result.reason();
            rebuildWidgets();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (isNotAnimating()) renderBackground(graphics);
        beginUiScale(graphics);
        applyZoom(graphics, partialTick);
        int uiMouseX = (int) toUiX(mouseX);
        int uiMouseY = (int) toUiY(mouseY);
        if (editing || selected() != null) renderPlayerPreview(graphics, partialTick);
        resetGuiRenderState(graphics);
        renderPanels(graphics);
        super.render(graphics, uiMouseX, uiMouseY, partialTick);
        resetGuiRenderState(graphics);
        endUiScale(graphics);
    }

    private void renderPanels(GuiGraphics graphics) {
        int top = panelTop();
        int left = 12;
        int right = getUiWidth() - 153;
        RenderSystem.setShaderColor(1, 1, 1, 1);
        graphics.blit(MENU_BIG, left, top, 0, 0, 141, 213, 256, 256);
        graphics.blit(MENU_BIG, right, top, 0, 0, 141, 213, 256, 256);
        graphics.blit(MENU_BIG, left + 17, top + 10, 142, 22, 107, 21, 256, 256);
        graphics.blit(MENU_BIG, right + 17, top + 10, 142, 22, 107, 21, 256, 256);
        if (editing) renderEditorText(graphics, left, right, top);
        else renderListText(graphics, left, right, top);
    }

    private void renderListText(GuiGraphics graphics, int left, int right, int top) {
        title(graphics, tr("gui.unofficialdmzaddon.custom_forms.list"), left + 70, top + 17);
        title(graphics, tr("gui.unofficialdmzaddon.custom_forms.preview"), right + 70, top + 17);
        List<CustomFormDefinition> forms = forms();
        for (int i = 0; i < Math.min(forms.size(), 12); i++) {
            int color = i == selectedIndex ? 0xFFFFD700 : 0xFFFFFFFF;
            TextUtil.drawStringWithBorder(graphics, font, forms.get(i).name(), left + 17, top + 41 + i * 10, color);
        }
        if (forms.isEmpty()) centered(graphics, tr("gui.unofficialdmzaddon.custom_forms.empty"), left + 70, top + 74, 0xFF999999);
        CustomFormDefinition selected = selected();
        if (selected != null) {
            centered(graphics, Component.literal(selected.name()), right + 70, top + 43, 0xFFFFFFFF);
            centered(graphics, tr("gui.unofficialdmzaddon.custom_forms.multiplier", format(selected.multiplier())), right + 70, top + 57, 0xFF7CFDD6);
            centered(graphics, tr("gui.unofficialdmzaddon.custom_forms.drain", String.format(Locale.US, "%.2f", selected.energyDrain())), right + 70, top + 70, 0xFF7CFDD6);
        }
    }

    private void renderEditorText(GuiGraphics graphics, int left, int right, int top) {
        Component pageTitle = page == 3 && !isSaiyan()
                ? tr("gui.unofficialdmzaddon.custom_forms.page.3.no_tail")
                : tr("gui.unofficialdmzaddon.custom_forms.page." + page);
        title(graphics, pageTitle, left + 70, top + 17);
        title(graphics, tr("gui.unofficialdmzaddon.custom_forms.summary"), right + 70, top + 17);
        if (page == 0) {
            centered(graphics, tr("gui.unofficialdmzaddon.custom_forms.name"), left + 70, top + 53, 0xFF9BFF9B);
            centered(graphics, tr("gui.unofficialdmzaddon.custom_forms.race", race()), left + 70, top + 105, 0xFFFFFFFF);
        } else if (page == 1) {
            centered(graphics, tr("gui.unofficialdmzaddon.custom_forms.hair", HAIR_TYPES[hairIndex]), left + 70, top + 61, 0xFFFFFFFF);
            centered(graphics, tr("gui.unofficialdmzaddon.custom_forms.hair_color"), left + 70, top + 81, 0xFF9BFF9B);
            centered(graphics, Component.literal(hairColor), left + 70, top + 113, 0xFFFFFFFF);
        } else if (page == 2 || page == 3) {
            centered(graphics, tr("gui.unofficialdmzaddon.custom_forms.colors_hint"), left + 70,
                    top + (page == 2 ? 119 : 109), 0xFF9BFF9B);
            centered(graphics, Component.literal(targetColor()), left + 70, top + 180, 0xFFFFFFFF);
        } else {
            centered(graphics, tr("gui.unofficialdmzaddon.custom_forms.multiplier", format(multiplier)), left + 70, top + 57, 0xFFFFFFFF);
            centered(graphics, tr("gui.unofficialdmzaddon.custom_forms.drain", String.format(Locale.US, "%.2f", drain)), left + 70, top + 94, 0xFFFFFFFF);
            centeredWrapped(graphics, tr("gui.unofficialdmzaddon.custom_forms.balance_hint"), left + 70, top + 119, 112, 0xFF9BFF9B);
        }
        centered(graphics, Component.literal(formName), right + 70, top + 44, 0xFFFFFFFF);
        centered(graphics, tr("gui.unofficialdmzaddon.custom_forms.multiplier", format(multiplier)), right + 70, top + 61, 0xFF7CFDD6);
        centered(graphics, tr("gui.unofficialdmzaddon.custom_forms.drain", String.format(Locale.US, "%.2f", drain)), right + 70, top + 76, 0xFF7CFDD6);
        centered(graphics, tr("gui.unofficialdmzaddon.custom_forms.cost", pendingCost()), right + 70, top + 103, pendingCost() <= availableTp() ? 0xFFFFD700 : 0xFFFF5555);
        centered(graphics, tr("gui.unofficialdmzaddon.custom_forms.available", availableTp()), right + 70, top + 118, 0xFFFFFFFF);
        centered(graphics, tr("gui.unofficialdmzaddon.custom_forms.page_count", page + 1, PAGE_COUNT), right + 70, top + 146, 0xFFAAAAAA);
        if (!resultKey.isEmpty()) {
            centeredWrapped(graphics, tr(resultKey, resultCost), right + 70, top + 158, 116,
                    resultKey.contains("error") ? 0xFFFF5555 : 0xFF7CFDD6);
        }
    }

    private void renderPlayerPreview(GuiGraphics graphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        CustomFormDefinition preview = editing ? draft() : selected();
        if (preview == null || race().isEmpty()) return;

        final String[] oldGroup = {""};
        final String[] oldForm = {""};
        final boolean[] applied = {false};
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            oldGroup[0] = data.getCharacter().getActiveFormGroup();
            oldForm[0] = data.getCharacter().getActiveForm();
            installPreview(preview);
            data.getCharacter().setActiveForm(PREVIEW_GROUP, PREVIEW_FORM);
            applied[0] = true;
        });
        if (!applied[0]) return;

        int centerX = getUiWidth() / 2;
        int baseY = getUiHeight() / 2 + 88;
        int scale = 74;
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf camera = new Quaternionf().rotateX(0);
        pose.mul(camera);
        float body = player.yBodyRot, bodyOld = player.yBodyRotO, yaw = player.getYRot(), yawOld = player.yRotO;
        float pitch = player.getXRot(), pitchOld = player.xRotO, head = player.yHeadRot, headOld = player.yHeadRotO;
        player.yBodyRot = playerRotation;
        player.yBodyRotO = playerRotation;
        player.setYRot(playerRotation);
        player.yRotO = playerRotation;
        player.setXRot(playerPitch);
        player.xRotO = playerPitch;
        player.yHeadRot = playerRotation;
        player.yHeadRotO = playerRotation;
        Matrix4f projection = new Matrix4f().ortho(0, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), 0, -10000, 10000);
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 320);
        DMZSkinLayer.PREVIEW_MODE = true;
        try {
            InventoryScreen.renderEntityInInventory(graphics, centerX, baseY, scale, pose, camera, player);
            // Aura rendering uses its own shader and buffered render types. Flush it while the
            // preview form is still active so its state can never tint DMZ's menu textures.
            if (editing && page == 3) {
                AuraRenderer.renderGuiAura(player, graphics.pose(), projection, centerX, baseY, scale, partialTick, true);
            }
            graphics.flush();
        } finally {
            DMZSkinLayer.PREVIEW_MODE = false;
            graphics.pose().popPose();
            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> data.getCharacter().setActiveForm(oldGroup[0], oldForm[0]));
            if (ConfigManager.getAllFormsForRace(race()) != null) ConfigManager.getAllFormsForRace(race()).remove(PREVIEW_GROUP);
            player.yBodyRot = body;
            player.yBodyRotO = bodyOld;
            player.setYRot(yaw);
            player.yRotO = yawOld;
            player.setXRot(pitch);
            player.xRotO = pitchOld;
            player.yHeadRot = head;
            player.yHeadRotO = headOld;
            resetGuiRenderState(graphics);
        }
    }

    private void resetGuiRenderState(GuiGraphics graphics) {
        graphics.flush();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        resetRawRenderState();
    }

    private void resetRawRenderState() {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    private void installPreview(CustomFormDefinition preview) {
        var raceForms = ConfigManager.getAllFormsForRace(race());
        if (raceForms == null) return;
        FormConfig group = new FormConfig();
        group.setConfigVersion(FormConfig.CURRENT_VERSION);
        group.setGroupName(PREVIEW_GROUP);
        group.setFormType("superforms");
        FormConfig.FormData data = preview.toFormData();
        data.setName(PREVIEW_FORM);
        LinkedHashMap<String, FormConfig.FormData> forms = new LinkedHashMap<>();
        forms.put(PREVIEW_FORM, data);
        group.setForms(forms);
        raceForms.put(PREVIEW_GROUP, group);
    }

    private int panelTop() { return getUiHeight() / 2 - 106; }
    private String format(double value) { return String.format(Locale.US, "%.1f", value); }
    private void title(GuiGraphics graphics, Component text, int x, int y) { centered(graphics, text, x, y, 0xFFFFD700); }
    private void centered(GuiGraphics graphics, Component text, int x, int y, int color) { TextUtil.drawCenteredStringWithBorder(graphics, font, text, x, y, color); }
    private void centeredWrapped(GuiGraphics graphics, Component text, int centerX, int y, int maxWidth, int color) {
        List<FormattedCharSequence> lines = font.split(text, maxWidth);
        for (int i = 0; i < lines.size(); i++) {
            TextUtil.drawCenteredStringWithBorder(graphics, font, lines.get(i), centerX, y + i * (font.lineHeight + 1), color);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        double x = toUiX(mouseX);
        double y = toUiY(mouseY);
        if (!editing && x >= 29 && x <= 136) {
            int index = (int) ((y - (panelTop() + 38)) / 10);
            if (index >= 0 && index < Math.min(forms().size(), 12)) {
                selectedIndex = index;
                loadDraft(forms().get(index));
                rebuildWidgets();
                return true;
            }
        }
        int previewLeft = 165;
        int previewRight = getUiWidth() - 165;
        if (editing && button == 0 && x >= previewLeft && x <= previewRight && y >= panelTop() + 25 && y <= panelTop() + 182) {
            draggingModel = true;
            lastMouseX = x;
            lastMouseY = y;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingModel && button == 0) {
            double x = toUiX(mouseX), y = toUiY(mouseY);
            playerRotation -= (float) (x - lastMouseX);
            playerPitch = Mth.clamp(playerPitch + (float) (y - lastMouseY), -45.0F, 45.0F);
            lastMouseX = x;
            lastMouseY = y;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingModel = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && editing) {
            previousPage();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        resetRawRenderState();
        super.onClose();
    }
}
