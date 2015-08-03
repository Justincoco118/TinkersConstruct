package tconstruct.library.client;

import com.google.common.collect.Lists;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.util.Point;

import java.util.List;

import tconstruct.library.TinkerRegistry;
import tconstruct.library.materials.Material;
import tconstruct.library.tinkering.TinkersItem;

public class ToolBuildGuiInfo {

  private static final Material RenderMaterials[];

  public final ItemStack tool;
  // the positions where the slots are located
  public final List<Point> positions = Lists.newArrayList();;

  public ToolBuildGuiInfo() {
    // for repairing
    this.tool = null;
  }

  public ToolBuildGuiInfo(TinkersItem tool) {
    List<Material> mats = Lists.newLinkedList();
    for(int i = 0; i < tool.requiredComponents.length; i++) {
      mats.add(RenderMaterials[i%RenderMaterials.length]);
    }

    this.tool = tool.buildItemForRendering(mats);
  }

  /**
   * Add another slot at the specified position for the tool.
   * The positions are usually located between:
   *   X: 7 - 69
   *   Y: 18 - 64
   */
  public void addSlotPosition(int x, int y) {
    positions.add(new Point(x, y));
  }

  static {
    RenderMaterials = new Material[4];
    RenderMaterials[0] = new Material("_internal_render1");
    RenderMaterials[0].setRenderInfo(0xc1c1c1, EnumChatFormatting.WHITE);
    RenderMaterials[1] = new Material("_internal_render2");
    RenderMaterials[1].setRenderInfo(0x684e1e, EnumChatFormatting.WHITE);
    RenderMaterials[2] = new Material("_internal_render3");
    RenderMaterials[2].setRenderInfo(0x2376dd, EnumChatFormatting.WHITE);
    RenderMaterials[3] = new Material("_internal_render4");
    RenderMaterials[3].setRenderInfo(0x8f60d4, EnumChatFormatting.WHITE);

    for(Material mat : RenderMaterials) {
      // yes, these will only be registered clientside
      // but it shouldn't matter because they're never used serverside and we don't use indices
      TinkerRegistry.addMaterial(mat);
    }
  }
}
