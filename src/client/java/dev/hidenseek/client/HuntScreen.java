package dev.hidenseek.client;

import com.google.gson.*;
import dev.hidenseek.core.Geometry.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import java.util.*;
import static dev.hidenseek.client.HuntClient.*;

public class HuntScreen extends Screen {
 private int tab=0,hintIndex=0,targetIndex=0,layer=-65,replacement=-1;
 private double zoom=1,panX,panZ;
 private int scroll;
 private JsonObject preview;
 private String lastDynamic="";
 private TextFieldWidget param,label;
 private String parameterValue;
 private final List<String> hints=List.of("animal","block","biome","east","south","above","sector","circle","rectangle","distance","firework","screenshot","bearing","biome_is","structure");
 public HuntScreen(){super(Text.translatable("hunt.title"));}
 public HuntScreen(int initialTab){this();tab=initialTab;}
 public boolean shouldPause(){return false;}
 public void tick(){String dynamic=string(view,"phase","")+view.get("hand")+view.get("offer");if(!dynamic.equals(lastDynamic)){lastDynamic=dynamic;if(tab==2||tab==3)init();}}
 private void button(int x,int y,int w,String key,Runnable action){addDrawableChild(ButtonWidget.builder(Text.translatable(key),b->action.run()).dimensions(x,y,w,20).build());}
 private void cardButton(int x,int y,int w,JsonObject card,Runnable action){String id=card.get("kind").getAsString();Text name=Text.translatable("card.hidenseek."+id);if(view.has("cardDefinitions")&&view.getAsJsonObject("cardDefinitions").has(id)){var definition=view.getAsJsonObject("cardDefinitions").getAsJsonObject(id);var names=definition.getAsJsonObject("names");name=Text.literal(string(names,client.options.language,string(names,"en_us",id))+" · "+definition.get("durationSeconds").getAsInt()+"s");}addDrawableChild(ButtonWidget.builder(name,b->action.run()).dimensions(x,y,w,20).build());}
 private void changeTab(int next){if(param!=null)parameterValue=param.getText();tab=next;scroll=0;init();}
 protected void init(){
  clearChildren();int y=38;
  button(12,y,90,"hunt.map",()->changeTab(0));button(106,y,90,"hunt.hints",()->changeTab(1));button(200,y,90,"hunt.cards",()->changeTab(2));button(294,y,90,"hunt.kits",()->changeTab(3));
  button(width-86,10,74,"gui.done",this::close);
  if(tab==0){button(width-100,68,88,"hunt.layer",()->{layer=layer< -64?-64:layer>=304?-65:layer+16;});button(width-100,92,88,"hunt.reset",()->{zoom=1;panX=panZ=0;});
   label=new TextFieldWidget(textRenderer,width-130,122,118,20,Text.translatable("hunt.mark"));label.setMaxLength(32);label.setText("?");addDrawableChild(label);
  }
  if(tab==1){
   button(12,68,34,"<",()->{hintIndex=Math.floorMod(hintIndex-1,hints.size());preview=null;parameterValue=null;init();});button(300,68,34,">",()->{hintIndex=(hintIndex+1)%hints.size();preview=null;parameterValue=null;init();});
   param=new TextFieldWidget(textRenderer,12,118,Math.min(width-24,380),20,Text.translatable("hunt.parameters"));param.setMaxLength(200);param.setText(parameterValue==null?defaultParam():parameterValue);addDrawableChild(param);
   button(12,145,132,"hunt.preview",()->{preview=makeHint();if(preview!=null){var quote=preview.deepCopy();quote.addProperty("action","quote");intent(quote);}});
   button(150,145,132,"hunt.buy",()->{if(preview!=null&&view.has("quote")&&string(view.getAsJsonObject("quote"),"kind","").equals(string(preview,"kind",""))){preview.addProperty("quotedTier",view.getAsJsonObject("quote").get("tier").getAsInt());preview.addProperty("request",UUID.randomUUID().toString());intent(preview);preview=null;}});
   button(12,218,260,"hunt.challenge",()->intent(action("challenge")));
   button(12,244,260,"hunt.antidote",()->intent(action("antidote")));
   button(278,244,140,"hunt.wings",()->intent(action("wings")));
  }
  if(tab==2){
   button(12,68,200,"hunt.next_target",()->{targetIndex++;});
   button(220,68,100,"hunt.watch",()->{JsonObject o=action("camera");o.addProperty("target",target());intent(o);});
   button(326,68,100,"hunt.return",()->{intent(action("camera"));});
   if(view.has("hand")){int i=0;for(var e:view.getAsJsonArray("hand")){var card=e.getAsJsonObject();int row=i++;cardButton(12,120+row*23,260,card,()->{var o=action("card");o.addProperty("card",card.get("id").getAsString());o.addProperty("target",target());intent(o);});}}
   if(view.has("offer")){int i=0;for(var e:view.getAsJsonArray("offer")){int index=i++;var card=e.getAsJsonObject();cardButton(290,120+index*25,Math.max(110,width-302),card,()->{var o=action("choose");o.addProperty("index",index);o.addProperty("offer",string(view,"offerId",""));o.addProperty("replace",replacement);intent(o);});}button(290,230,Math.max(110,width-302),"hunt.replace",()->{replacement++;if(replacement>=view.getAsJsonArray("hand").size())replacement=-1;});}
  }
  if(tab==3&&view.has("participant")){int i=0;for(var k:view.getAsJsonObject("participant").getAsJsonArray("kits")){String kit=k.getAsString();button(12,90+i++*28,260,"kit.hidenseek."+kit,()->{var o=action("kit");o.addProperty("kit",kit);intent(o);});}button(12,222,260,"hunt.kit_power",()->intent(action("kit_power")));}
 }
 private String target(){if(!view.has("seekers")||view.getAsJsonArray("seekers").isEmpty())return "";return view.getAsJsonArray("seekers").get(Math.floorMod(targetIndex,view.getAsJsonArray("seekers").size())).getAsString();}
 private String defaultParam(){int x=(int)number(view,"originX",0),z=(int)number(view,"originZ",0);return switch(hints.get(hintIndex)){case "block"->"minecraft:stone";case "biome_is"->"minecraft:plains";case "above"->"64";case "circle"->(x+256)+" "+(z+256)+" 128";case "rectangle","sector"->x+" "+z+" "+(x+128)+" "+(z+128);case "distance"->"128";default->"";};}
 private JsonObject makeHint(){try{
  var o=action("hint");String k=hints.get(hintIndex);o.addProperty("kind",k);String[] p=param.getText().trim().split("\\s+");
  switch(k){case "block"->o.addProperty("block",param.getText().trim());case "biome_is"->o.addProperty("biome",param.getText().trim());case "above"->o.addProperty("y",Double.parseDouble(p[0]));case "distance"->o.addProperty("radius",Double.parseDouble(p[0]));case "circle","rectangle","sector"->{o.addProperty("x",Double.parseDouble(p[0]));o.addProperty("z",Double.parseDouble(p[1]));if(k.equals("circle"))o.addProperty("radius",Double.parseDouble(p[2]));else{o.addProperty("x2",Double.parseDouble(p[2]));o.addProperty("z2",Double.parseDouble(p[3]));}}}
  return o;
 }catch(RuntimeException ex){return null;}}
 public void render(DrawContext ctx,int mx,int my,float delta){
  ctx.fill(0,0,width,height,0xee101820);ctx.drawTextWithShadow(textRenderer,title,12,12,0xffe2b75a);
  String seconds=String.format(Locale.ROOT,"%02d:%02d",(int)number(view,"seconds",0)/60,(int)number(view,"seconds",0)%60);
  ctx.drawTextWithShadow(textRenderer,Text.literal(string(view,"phase","")+" • "+seconds+" • "+(view.has("intel")?"◆ "+view.get("intel").getAsInt():string(view,"role",""))),140,15,0xffeeeeee);
  if(tab!=0){ctx.enableScissor(0,64,width,height-15);ctx.getMatrices().pushMatrix();ctx.getMatrices().translate(0,-scroll);}
  if(tab==0)renderMap(ctx,mx,my);
  if(tab==1){
   String k=hints.get(hintIndex);ctx.drawTextWithShadow(textRenderer,Text.translatable("hint.hidenseek."+k),54,75,0xffffffff);
   int tier=view.has("tiers")&&view.getAsJsonObject("tiers").has(k)?view.getAsJsonObject("tiers").get(k).getAsInt():0;
   if(preview!=null&&view.has("quote")&&string(view.getAsJsonObject("quote"),"kind","").equals(k))tier=view.getAsJsonObject("quote").get("tier").getAsInt();
   ctx.drawTextWithShadow(textRenderer,"☠".repeat(tier)+"  ◆ "+(tier>0?view.getAsJsonArray("costs").get(tier-1).getAsInt():0),12,98,0xffe2b75a);
   if(preview!=null)ctx.drawTextWithShadow(textRenderer,Text.translatable("hunt.preview_ready",param.getText()),12,176,0xff88eeaa);
   ctx.drawTextWithShadow(textRenderer,Text.translatable("hunt.parameter_help"),12,193,0xffaab8c5);
   if(view.has("history")){int y=278;var history=view.getAsJsonArray("history");for(int i=history.size()-1;i>=0;i--){var h=history.get(i).getAsJsonObject();ctx.drawTextWithShadow(textRenderer,Text.translatable("hint.hidenseek."+h.get("kind").getAsString()).append(": "+h.get("answer").getAsString()),12,y,0xffcccccc);y+=13;}}
  }
  if(tab==2){String targetName=target();if(view.has("positions"))for(var e:view.getAsJsonArray("positions")){var p=e.getAsJsonObject();if(string(p,"id","").equals(targetName)){targetName=string(p,"name",targetName);break;}}ctx.drawTextWithShadow(textRenderer,Text.literal(targetName),12,98,0xffaab8c5);ctx.drawWrappedTextWithShadow(textRenderer,Text.translatable("hunt.offer_note"),12,305,width-24,0xffaab8c5);ctx.drawTextWithShadow(textRenderer,"↔ "+(replacement+1),290,255,0xffe2b75a);}
  if(tab==3&&view.has("participant"))ctx.drawTextWithShadow(textRenderer,Text.translatable("hunt.selected",string(view.getAsJsonObject("participant"),"kit","")),12,200,0xffaab8c5);
  if(tab==0&&view.has("participant")){var p=view.getAsJsonObject("participant");int y=198;long now=(long)number(view,"tick",0);for(var effect:p.getAsJsonObject("effects").entrySet()){long left=(effect.getValue().getAsLong()-now)/20;if(left>0&&y<height-16){ctx.drawTextWithShadow(textRenderer,effect.getKey()+": "+left+"s",width-145,y,0xffe2b75a);y+=12;}}if(!string(p,"taskId","").isEmpty())ctx.drawTextWithShadow(textRenderer,string(p,"taskId","")+": "+(int)number(p,"taskProgress",0),width-145,y,0xff88dddd);}
  for(var child:children())if(child instanceof ClickableWidget widget&&widget.getY()>=64)widget.render(ctx,mx,my+scroll,delta);
  if(tab!=0){ctx.getMatrices().popMatrix();ctx.disableScissor();ctx.drawTextWithShadow(textRenderer,Text.translatable("hunt.scroll"),12,height-12,0xffaab8c5);}
  for(var child:children())if(child instanceof ClickableWidget widget&&widget.getY()<64)widget.render(ctx,mx,my,delta);
 }
 private int mapSide(){return Math.max(64,Math.min(width-160,height-120));}
 private double scale(){return mapSide()/number(view,"size",1024)*zoom;}
 private int sx(double x){return (int)(12+(x-number(view,"originX",0))*scale()+panX);}
 private int sz(double z){return (int)(68+(z-number(view,"originZ",0))*scale()+panZ);}
 private void renderMap(DrawContext ctx,int mx,int my){
  if(!view.has("size"))return;int side=mapSide();ctx.enableScissor(12,68,12+side,68+side);
  int originX=(int)number(view,"originX",0),originZ=(int)number(view,"originZ",0),size=(int)number(view,"size",1024),cell=(int)number(view,"cell",16);
  List<Constraint> constraints=new ArrayList<>();if(view.has("constraints"))for(var e:view.getAsJsonArray("constraints"))constraints.add(new Gson().fromJson(e,Constraint.class));
  for(int x=originX;x<originX+size;x+=cell)for(int z=originZ;z<originZ+size;z+=cell){
   double lo=layer< -64?number(view,"minY",-64):layer,hi=layer< -64?number(view,"maxY",320):layer+16;
   Box box=new Box(x,lo,z,x+cell,hi,z+cell);String reason="";
   for(var c:constraints)if(c.excludes(box)){reason=c.kind();break;}
   boolean excluded=!reason.isEmpty();var tile=terrain.get(x+":"+z);int color=excluded?0xff622f3c:tile!=null?tile.get("color").getAsInt():((x/cell+z/cell)%2==0?0xff354b57:0xff304550);
   ctx.fill(sx(x),sz(z),sx(x+cell),sz(z+cell),color);
   if(excluded){ctx.fill(sx(x),sz(z),sx(x)+1,sz(z+cell),0xff994555);}
   if(mx>=sx(x)&&mx<sx(x+cell)&&my>=sz(z)&&my<sz(z+cell)&&!reason.isEmpty())hoverReason=reason;
  }
  if(preview!=null&&Set.of("circle","rectangle","sector").contains(string(preview,"kind",""))){
   double a=number(preview,"x",0),b=number(preview,"z",0);if(preview.has("radius")){double r=number(preview,"radius",128);for(int i=0;i<120;i++){double t=i*Math.PI/60;ctx.fill(sx(a+Math.cos(t)*r),sz(b+Math.sin(t)*r),sx(a+Math.cos(t)*r)+2,sz(b+Math.sin(t)*r)+2,0xffffff44);}}
   else{int left=sx(a),top=sz(b),right=sx(number(preview,"x2",a)),bottom=sz(number(preview,"z2",b));ctx.drawBorder(left,top,right-left,bottom-top,0xffffff44);}
  }
  if(view.has("marks"))for(var e:view.getAsJsonArray("marks")){var m=e.getAsJsonObject();int x=sx(number(m,"x",0)),z=sz(number(m,"z",0));ctx.drawTextWithShadow(textRenderer,"◇"+string(m,"label",""),x,z,0xffffcc55);}
  if(view.has("positions"))for(var e:view.getAsJsonArray("positions")){var p=e.getAsJsonObject();ctx.fill(sx(number(p,"x",0))-2,sz(number(p,"z",0))-2,sx(number(p,"x",0))+3,sz(number(p,"z",0))+3,0xff6de4df);}
  ctx.disableScissor();ctx.drawWrappedTextWithShadow(textRenderer,Text.translatable("hunt.map_legend"),12,78+side,width-24,0xffaab8c5);
  ctx.drawTextWithShadow(textRenderer,layer< -64?"Y: ALL":"Y: "+layer+"…"+(layer+16),width-130,153,0xffeeeeee);
  if(!hoverReason.isEmpty()){ctx.drawTextWithShadow(textRenderer,Text.translatable("hint.hidenseek."+hoverReason),width-130,175,0xffeeaaaa);hoverReason="";}
 }
 private String hoverReason="";
 public boolean mouseClicked(double x,double y,int button){
  if(tab==0&&x>=12&&x<12+mapSide()&&y>=68&&y<68+mapSide()&&(button==0||button==1)){
   var o=action("mark");o.addProperty("x",(x-12-panX)/scale()+number(view,"originX",0));o.addProperty("z",(y-68-panZ)/scale()+number(view,"originZ",0));o.addProperty("label",label.getText());o.addProperty("team",button==1);intent(o);return true;
  }return super.mouseClicked(x,tab!=0&&y>=64?y+scroll:y,button);
 }
 public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(tab==0&&button==2){panX+=dx;panZ+=dy;return true;}return super.mouseDragged(x,y,button,dx,dy);}
 public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(tab==0){zoom=MathHelper.clamp(zoom*Math.pow(1.2,vertical),.5,8);return true;}int content=tab==1?300+(view.has("history")?view.getAsJsonArray("history").size()*13:0):350;scroll=MathHelper.clamp(scroll-(int)(vertical*24),0,Math.max(0,content-height+24));return true;}
}

