/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.client.screen;

import com.mojang.logging.LogUtils;
import ivorius.psychedelicraft.client.screen.settings.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.*;
import net.minecraft.world.item.CreativeModeTabs;
import java.util.*;

/** A Forge-native options tree; changes are live and the session saves when its last page closes. */
public final class SettingsScreen extends Screen {
    private static final String GUI="gui.psychedelicraft.settings.";
    private static final class Session {
        final SettingsCatalog catalog=new SettingsCatalog(Minecraft.getInstance().level==null||Minecraft.getInstance().hasSingleplayerServer(),()->CreativeModeTabs.CACHED_PARAMETERS=null);
        boolean navigating;
    }
    private final Screen parent;
    private final Session session;
    private final SettingsCatalog.Page page;
    private final List<SettingsCatalog.Setting> settings;
    private SettingsList list;
    private Button reset;
    private List<net.minecraft.util.FormattedCharSequence> caption=List.of();
    public SettingsScreen(Screen parent){this(parent,new Session());}
    private SettingsScreen(Screen parent,Session session){this(parent,session,session.catalog.root());}
    private SettingsScreen(Screen parent,Session session,SettingsCatalog.Page page){
        super(Component.translatable(page.titleKey()));this.parent=parent;this.session=session;this.page=page;settings=page.settings();
    }
    @Override protected void init(){
        double scroll=list==null?0:list.getScrollAmount();
        Component note=page.id().equals("root")?Component.translatable(GUI+"choose_category")
                :Component.translatable(GUI+(page.needsRestart()?"restart_note":page.worldSettings()?"world_note":"client_note"));
        caption=font.split(note,Math.max(160,width-30));
        list=addRenderableWidget(new SettingsList(32+caption.size()*font.lineHeight));
        for(var entry:page.entries())list.add(entry);
        list.setScrollAmount(scroll);
        int span=Math.min(310,width-20),half=(span-6)/2,left=(width-span)/2;
        reset=addRenderableWidget(Button.builder(Component.translatable(GUI+"reset_page"),b->{settings.forEach(SettingsCatalog.Setting::reset);rebuildWidgets();})
                .bounds(left,height-26,half,20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE,b->onClose()).bounds(left+half+6,height-26,half,20).build());
    }
    private void navigate(Screen next){
        session.navigating=next instanceof SettingsScreen other&&other.session==session;
        try{minecraft.setScreen(next);}finally{session.navigating=false;}
    }
    @Override public void onClose(){navigate(parent);}
    @Override public void removed(){
        if(!session.navigating)try{session.catalog.save();}catch(RuntimeException error){
            LogUtils.getLogger().error("Could not save Psychedelicraft settings",error);
            SystemToast.add(minecraft.getToasts(),SystemToast.SystemToastIds.PERIODIC_NOTIFICATION,Component.translatable(GUI+"save_failed"),Component.translatable(GUI+"save_failed_detail"));
        }
    }
    @Override public void tick(){if(list!=null)for(var row:list.children())for(var child:row.widgets)if(child instanceof EditBox edit)edit.tick();}
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial){
        renderBackground(graphics);reset.active=settings.stream().anyMatch(s->!s.isDefault());super.render(graphics,mouseX,mouseY,partial);
        graphics.drawCenteredString(font,title,width/2,10,0xffffff);
        for(int i=0;i<caption.size();i++)graphics.drawCenteredString(font,caption.get(i),width/2,25+i*font.lineHeight,0xb0b0b0);
    }
    private final class SettingsList extends ContainerObjectSelectionList<Row> {
        SettingsList(int top){super(SettingsScreen.this.minecraft,SettingsScreen.this.width,SettingsScreen.this.height,top,SettingsScreen.this.height-34,44);}
        void add(SettingsCatalog.Entry entry){addEntry(new Row(entry));}
        @Override public int getRowWidth(){return Math.max(160,Math.min(560,SettingsScreen.this.width-40));}
        @Override protected int getScrollbarPosition(){return width/2+getRowWidth()/2+4;}
    }
    private final class Row extends ContainerObjectSelectionList.Entry<Row> {
        final SettingsCatalog.Entry entry;
        final List<AbstractWidget> widgets;
        final AbstractWidget control;
        final Button resetSetting;
        Row(SettingsCatalog.Entry entry){
            this.entry=entry;
            if(entry instanceof SettingsCatalog.Page child){
                control=Button.builder(Component.translatable(GUI+"category",Component.translatable(child.titleKey())),b->navigate(new SettingsScreen(SettingsScreen.this,session,child))).bounds(0,0,200,20).build();
                resetSetting=null;widgets=List.of(control);return;
            }
            var setting=(SettingsCatalog.Setting)entry;
            if(setting.toggle())control=CycleButton.onOffBuilder(setting.enabled()).displayOnlyValue().create(0,0,200,20,Component.translatable(setting.titleKey()),(b,value)->setting.setEnabled(value));
            else if(setting.slider())control=new ValueSlider(setting);
            else{
                var field=new EditBox(font,0,0,200,20,Component.translatable(setting.titleKey()));
                field.setMaxLength(setting.selectors()?Integer.MAX_VALUE:128);field.setValue(setting.text());
                field.setResponder(text->field.setTextColor(setting.setText(text)?0xffffff:0xff6666));control=field;
            }
            if(setting.selectors())control.setTooltip(Tooltip.create(Component.translatable(GUI+"selectors")));
            else if(!setting.toggle())control.setTooltip(Tooltip.create(Component.translatable(GUI+"range",setting.minimumText(),setting.maximumText()).append("\n").append(Component.translatable(GUI+"invalid_hint"))));
            resetSetting=Button.builder(Component.translatable("controls.reset"),b->{setting.reset();rebuildWidgets();}).bounds(0,0,50,20).build();
            resetSetting.setTooltip(Tooltip.create(Component.translatable(GUI+"reset_one")));
            widgets=List.of(control,resetSetting);
        }
        @Override public List<? extends GuiEventListener> children(){return widgets;}
        @Override public List<? extends NarratableEntry> narratables(){return widgets;}
        @Override public void render(GuiGraphics graphics,int index,int top,int left,int rowWidth,int rowHeight,int mouseX,int mouseY,boolean hovered,float partial){
            if(resetSetting==null){control.setX(left);control.setY(top+10);control.setWidth(rowWidth);}
            else{
                graphics.enableScissor(left,top,left+rowWidth,top+14);
                graphics.drawString(font,Component.translatable(entry.titleKey()),left,top+2,0xffffff);graphics.disableScissor();
                control.setX(left);control.setY(top+17);control.setWidth(rowWidth-56);
                resetSetting.setX(left+rowWidth-50);resetSetting.setY(top+17);resetSetting.active=!((SettingsCatalog.Setting)entry).isDefault();
            }
            widgets.forEach(widget->widget.render(graphics,mouseX,mouseY,partial));
        }
    }
    private final class ValueSlider extends AbstractSliderButton {
        private final SettingsCatalog.Setting setting;
        ValueSlider(SettingsCatalog.Setting setting){super(0,0,200,20,Component.empty(),(setting.number()-setting.minimum())/(setting.maximum()-setting.minimum()));this.setting=setting;updateMessage();}
        @Override protected void updateMessage(){setMessage(setting.zeroIsOff()&&setting.number()<.001?CommonComponents.OPTION_OFF:Component.literal(SettingInput.decimal(setting.number())));}
        @Override protected void applyValue(){setting.setNumber(setting.minimum()+value*(setting.maximum()-setting.minimum()));updateMessage();}
        @Override protected MutableComponent createNarrationMessage(){return Component.translatable("options.generic_value",Component.translatable(setting.titleKey()),getMessage());}
    }
}
