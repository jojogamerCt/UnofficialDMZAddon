package org.unofficial.unofficialdmzaddon.client;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.ColorSlider;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.util.BaseMenuScreen;
import com.dragonminez.client.render.layer.DMZSkinLayer;
import com.dragonminez.client.render.shader.TransformationMaskBufferSource;
import com.dragonminez.client.render.shader.TransformationMaskRenderState;
import com.dragonminez.mixin.client.PostChainAccessor;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.unofficial.unofficialdmzaddon.UnofficialDMZConfig;
import org.unofficial.unofficialdmzaddon.dmz.CustomFormDefinition;
import org.unofficial.unofficialdmzaddon.network.AddonNetwork;
import org.unofficial.unofficialdmzaddon.network.CustomFormSaveResultS2C;
import org.unofficial.unofficialdmzaddon.network.DeleteCustomFormC2S;
import org.unofficial.unofficialdmzaddon.network.SaveCustomFormC2S;
import org.unofficial.unofficialdmzaddon.network.SelectCustomFormC2S;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class CustomFormsScreen extends BaseMenuScreen {
    private static final ResourceLocation MENU_BIG = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menubig.png");
    private static final ResourceLocation BUTTONS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/characterbuttons.png");
    private static final ResourceLocation AURA_PREVIEW = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
            "textures/entity/races/aura/kakarot_aura.png");
    private static final int AURA_FRAME_SIZE = 1024;
    private static final int AURA_SHEET_WIDTH = AURA_FRAME_SIZE * 4;
    private static final String PREVIEW_GROUP = "unofficialdmzaddon_custom_form_preview";
    private static final String PREVIEW_FORM = "preview";
    private static final String[] HAIR_TYPES = {"base", "ssj", "ssj2", "ssj3"};
    private static final int PAGE_COUNT = 6;

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
    private boolean constantAura;
    private boolean auraOutlineEnabled;
    private String auraOutlineColor = "#37E8FF";
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
    private PostChain outlinePreviewChain;
    private TextureTarget outlinePreviewTarget;
    private int outlinePreviewWidth = -1;
    private int outlinePreviewHeight = -1;

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
        } else if (page == 4) {
            addRenderableWidget(textButton(left + 34, top + 47,
                    tr(constantAura
                            ? "gui.unofficialdmzaddon.custom_forms.constant_aura.on"
                            : "gui.unofficialdmzaddon.custom_forms.constant_aura.off"),
                    () -> { if (UnofficialDMZConfig.CUSTOM_FORMS_ALLOW_CONSTANT_AURA.get()) constantAura = !constantAura; rebuildWidgets(); }));
            addRenderableWidget(textButton(left + 34, top + 72,
                    tr(auraOutlineEnabled
                            ? "gui.unofficialdmzaddon.custom_forms.outline.on"
                            : "gui.unofficialdmzaddon.custom_forms.outline.off"),
                    () -> { if (UnofficialDMZConfig.CUSTOM_FORMS_ALLOW_AURA_OUTLINE.get()) auraOutlineEnabled = !auraOutlineEnabled; rebuildWidgets(); }));
            if (auraOutlineEnabled && UnofficialDMZConfig.CUSTOM_FORMS_ALLOW_AURA_OUTLINE.get()) {
                colorTarget = "outline";
                addRenderableWidget(colorButton(left + 34, top + 97, "outline"));
                initColorSliders(left + 21, top + 132);
            }
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
                eye1Color, eye2Color, auraColor, bodyColor, tailColor,
                constantAura, auraOutlineEnabled, auraOutlineColor, multiplier, drain);
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
        constantAura = form.constantAura();
        auraOutlineEnabled = form.auraOutlineEnabled();
        auraOutlineColor = form.auraOutlineColor();
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
        constantAura = false;
        auraOutlineEnabled = false;
        auraOutlineColor = "#37E8FF";
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
            case "outline" -> auraOutlineColor;
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
            case "outline" -> auraOutlineColor = color;
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
        } else if (page == 4) {
            if (auraOutlineEnabled && UnofficialDMZConfig.CUSTOM_FORMS_ALLOW_AURA_OUTLINE.get()) {
                centered(graphics, Component.literal(auraOutlineColor), left + 70, top + 180, 0xFFFFFFFF);
            } else {
                centeredWrapped(graphics, tr("gui.unofficialdmzaddon.custom_forms.aura_effects_hint"),
                        left + 70, top + 112, 112, 0xFF9BFF9B);
            }
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
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 320);
        DMZSkinLayer.PREVIEW_MODE = true;
        try {
            if (editing && (page == 3 || page == 4)) {
                renderAuraPreview(graphics, player, preview, centerX, baseY, scale, partialTick);
            }
            InventoryScreen.renderEntityInInventory(graphics, centerX, baseY, scale, pose, camera, player);
            graphics.flush();
            if (editing && page == 4 && preview.auraOutlineEnabled()
                    && UnofficialDMZConfig.CUSTOM_FORMS_ALLOW_AURA_OUTLINE.get()) {
                renderOutlinePreview(graphics, player, preview, centerX, baseY, scale, pose, camera, partialTick);
            }
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

    /**
     * Draws an animated aura frame without exposing the spritesheet's opaque black background.
     * Colored auras use additive blending; black uses a mask-like darkening blend so it remains visible.
     */
    private void renderAuraPreview(GuiGraphics graphics, Player player, CustomFormDefinition preview,
                                   int centerX, int baseY, int modelScale, float partialTick) {
        float animation = (player.tickCount + partialTick) * 0.5F;
        int frame = Math.floorMod((int) Math.floor(animation), 4);
        int nextFrame = (frame + 1) % 4;
        float frameBlend = animation - (float) Math.floor(animation);

        // The creator renders its character at roughly twice the visual scale of DMZ's regular
        // GUI aura target. Keep the aura proportional to that preview rather than torso-sized.
        int width = Math.round(modelScale * 4.5F);
        int height = Math.round(modelScale * 4.5F);
        int x = centerX - width / 2;
        int y = baseY - height + Math.round(modelScale * 0.45F);
        float[] rgb = ColorUtils.hexToRgb(preview.auraColor());
        boolean blackAura = Math.max(rgb[0], Math.max(rgb[1], rgb[2])) <= 0.02F;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.enableBlend();
        if (blackAura) {
            // Pure black adds no light, so additive blending made it disappear. This blend darkens
            // only the non-black pixels in DMZ's grayscale aura mask and keeps its background clear.
            RenderSystem.blendFunc(GL11.GL_ZERO, GL11.GL_ONE_MINUS_SRC_COLOR);
            drawBlackAuraPreviewFrame(graphics, x, y, width, height, frame, 0.82F * (1.0F - frameBlend));
            drawBlackAuraPreviewFrame(graphics, x, y, width, height, nextFrame, 0.82F * frameBlend);
        } else {
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            drawAuraPreviewFrame(graphics, rgb, x, y, width, height, frame, 0.82F * (1.0F - frameBlend));
            drawAuraPreviewFrame(graphics, rgb, x, y, width, height, nextFrame, 0.82F * frameBlend);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
    }

    private void drawAuraPreviewFrame(GuiGraphics graphics, float[] rgb, int x, int y,
                                      int width, int height, int frame, float alpha) {
        if (alpha <= 0.001F) return;
        RenderSystem.setShaderColor(rgb[0], rgb[1], rgb[2], alpha);
        graphics.blit(AURA_PREVIEW, x, y, width, height,
                frame * AURA_FRAME_SIZE, 0.0F, AURA_FRAME_SIZE, AURA_FRAME_SIZE,
                AURA_SHEET_WIDTH, AURA_FRAME_SIZE);
    }

    private void drawBlackAuraPreviewFrame(GuiGraphics graphics, int x, int y,
                                           int width, int height, int frame, float strength) {
        if (strength <= 0.001F) return;
        RenderSystem.setShaderColor(strength, strength, strength, 1.0F);
        graphics.blit(AURA_PREVIEW, x, y, width, height,
                frame * AURA_FRAME_SIZE, 0.0F, AURA_FRAME_SIZE, AURA_FRAME_SIZE,
                AURA_SHEET_WIDTH, AURA_FRAME_SIZE);
    }

    /**
     * Renders the creator preview through DragonMineZ's transformation-mask post shader without
     * ever running that post-chain against Minecraft's real main framebuffer. The chain renders
     * into a private black target and only its additive RGB result is composited back into the GUI.
     * This preserves DMZ's Ikari/custom-form outline while keeping the creator UI and model intact.
     */
    private void renderOutlinePreview(GuiGraphics graphics, Player player, CustomFormDefinition preview,
                                      int centerX, int baseY, int modelScale,
                                      Quaternionf pose, Quaternionf camera, float partialTick) {
        FormConfig.FormData formData = preview.toFormData();
        FormConfig.FormData.OutlineShaderConfig outline = formData.getOutlineShader();
        if (outline == null || !outline.isEnabled()) return;

        Minecraft minecraft = Minecraft.getInstance();
        PostChain chain = outlinePreviewChain(minecraft);
        if (chain == null || outlinePreviewTarget == null) return;
        var maskTarget = chain.getTempTarget("entity_mask");
        if (maskTarget == null) return;

        float[] primary = ColorUtils.hexToRgb(outline.getPrimaryColor());
        float[] secondary = ColorUtils.hexToRgb(outline.getSecondaryColor());
        TransformationMaskBufferSource maskSource = new TransformationMaskBufferSource();
        maskSource.setIncludeOriginal(false);
        maskSource.setEntityColors(primary[0], primary[1], primary[2],
                secondary[0], secondary[1], secondary[2]);

        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, baseY, 50.0D);
        graphics.pose().mulPoseMatrix(new Matrix4f().scaling(modelScale, modelScale, -modelScale));
        graphics.pose().mulPose(new Quaternionf(pose));
        Lighting.setupForEntityInInventory();
        dispatcher.overrideCameraOrientation(new Quaternionf(camera).conjugate());
        dispatcher.setRenderShadow(false);
        try {
            dispatcher.render(player, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F,
                    graphics.pose(), maskSource, 0xF000F0);
        } finally {
            dispatcher.setRenderShadow(true);
            graphics.pose().popPose();
            Lighting.setupFor3DItems();
        }

        outlinePreviewTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        outlinePreviewTarget.clear(Minecraft.ON_OSX);
        try {
            maskTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            maskTarget.clear(Minecraft.ON_OSX);
            maskTarget.copyDepthFrom(outlinePreviewTarget);
            TransformationMaskRenderState.setCurrentTargets(maskTarget);
            maskSource.endMaskBatch();
        } finally {
            TransformationMaskRenderState.setCurrentTargets(null);
        }

        configureOutlinePreviewUniforms(chain, outline, player.tickCount + partialTick);
        chain.process(partialTick);
        minecraft.getMainRenderTarget().bindWrite(false);
        compositeOutlinePreview(minecraft);
        resetRawRenderState();
    }

    private PostChain outlinePreviewChain(Minecraft minecraft) {
        int width = minecraft.getMainRenderTarget().width;
        int height = minecraft.getMainRenderTarget().height;
        if (outlinePreviewChain != null && outlinePreviewTarget != null
                && outlinePreviewWidth == width && outlinePreviewHeight == height) {
            return outlinePreviewChain;
        }
        closeOutlinePreviewChain();
        try {
            outlinePreviewTarget = new TextureTarget(width, height, true, Minecraft.ON_OSX);
            outlinePreviewTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            outlinePreviewChain = new PostChain(minecraft.getTextureManager(), minecraft.getResourceManager(),
                    outlinePreviewTarget,
                    ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "shaders/post/transformation_outline.json"));
            outlinePreviewChain.resize(width, height);
            outlinePreviewWidth = width;
            outlinePreviewHeight = height;
            return outlinePreviewChain;
        } catch (IOException | RuntimeException ignored) {
            closeOutlinePreviewChain();
            return null;
        }
    }

    private void compositeOutlinePreview(Minecraft minecraft) {
        if (outlinePreviewTarget == null) return;
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_ONE, GL11.GL_ONE);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, outlinePreviewTarget.getColorTextureId());

        Matrix4f identity = new Matrix4f();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.vertex(identity, 0.0F, height, 0.0F).uv(0.0F, 0.0F).endVertex();
        builder.vertex(identity, width, height, 0.0F).uv(1.0F, 0.0F).endVertex();
        builder.vertex(identity, width, 0.0F, 0.0F).uv(1.0F, 1.0F).endVertex();
        builder.vertex(identity, 0.0F, 0.0F, 0.0F).uv(0.0F, 1.0F).endVertex();
        BufferUploader.drawWithShader(builder.end());

        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private void configureOutlinePreviewUniforms(PostChain chain,
                                                 FormConfig.FormData.OutlineShaderConfig outline,
                                                 float animationTicks) {
        float animationTime = animationTicks / 20.0F;
        float blurRadius = Mth.clamp((float) outline.getOutlineThickness() * 3.0F, 2.0F, 14.0F);
        for (PostPass pass : ((PostChainAccessor) chain).dragonminez$getPasses()) {
            EffectInstance effect = pass.getEffect();
            String name = pass.getName();
            if ("dragonminez:transformation_unpack".equals(name)) {
                setUniform(effect, "AnimationTime", animationTime);
            } else if ("dragonminez:transformation_blur_h".equals(name)
                    || "dragonminez:transformation_blur_v".equals(name)) {
                setUniform(effect, "BloomRadius", blurRadius);
            } else if ("dragonminez:transformation_composite".equals(name)) {
                setUniform(effect, "BloomStrength", 0.95F);
                setUniform(effect, "GlowStrength", 1.35F);
            }
        }
    }

    private void setUniform(EffectInstance effect, String name, float value) {
        var uniform = effect.getUniform(name);
        if (uniform != null) uniform.set(value);
    }

    private void closeOutlinePreviewChain() {
        if (outlinePreviewChain != null) {
            outlinePreviewChain.close();
            outlinePreviewChain = null;
        }
        if (outlinePreviewTarget != null) {
            outlinePreviewTarget.destroyBuffers();
            outlinePreviewTarget = null;
        }
        outlinePreviewWidth = -1;
        outlinePreviewHeight = -1;
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
        group.setFormType("customforms");
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
    public void removed() {
        closeOutlinePreviewChain();
        super.removed();
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
