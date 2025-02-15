package slimeknights.tconstruct.plugin.jei;

import com.google.common.collect.ImmutableList;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaRecipeCategoryUid;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.SerializationTags;
import net.minecraft.tags.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.ModList;
import slimeknights.mantle.item.RetexturedBlockItem;
import slimeknights.mantle.recipe.helper.RecipeHelper;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.common.registration.CastItemObject;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.recipe.RecipeTypes;
import slimeknights.tconstruct.library.recipe.alloying.AlloyRecipe;
import slimeknights.tconstruct.library.recipe.casting.IDisplayableCastingRecipe;
import slimeknights.tconstruct.library.recipe.entitymelting.EntityMeltingRecipe;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuel;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipe;
import slimeknights.tconstruct.library.recipe.melting.MeltingRecipe;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.recipe.modifiers.severing.SeveringRecipe;
import slimeknights.tconstruct.library.recipe.molding.MoldingRecipe;
import slimeknights.tconstruct.library.recipe.partbuilder.IDisplayPartBuilderRecipe;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.nbt.MaterialIdNBT;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.plugin.jei.casting.CastingBasinCategory;
import slimeknights.tconstruct.plugin.jei.casting.CastingTableCategory;
import slimeknights.tconstruct.plugin.jei.entity.DefaultEntityMeltingRecipe;
import slimeknights.tconstruct.plugin.jei.entity.EntityIngredientHelper;
import slimeknights.tconstruct.plugin.jei.entity.EntityIngredientRenderer;
import slimeknights.tconstruct.plugin.jei.entity.EntityMeltingRecipeCategory;
import slimeknights.tconstruct.plugin.jei.entity.SeveringCategory;
import slimeknights.tconstruct.plugin.jei.melting.FoundryCategory;
import slimeknights.tconstruct.plugin.jei.melting.MeltingCategory;
import slimeknights.tconstruct.plugin.jei.melting.MeltingFuelHandler;
import slimeknights.tconstruct.plugin.jei.modifiers.ModifierBookmarkIngredientRenderer;
import slimeknights.tconstruct.plugin.jei.modifiers.ModifierIngredientHelper;
import slimeknights.tconstruct.plugin.jei.modifiers.ModifierRecipeCategory;
import slimeknights.tconstruct.plugin.jei.partbuilder.MaterialItemList;
import slimeknights.tconstruct.plugin.jei.partbuilder.PartBuilderCategory;
import slimeknights.tconstruct.plugin.jei.partbuilder.PatternIngredientHelper;
import slimeknights.tconstruct.plugin.jei.partbuilder.PatternIngredientRenderer;
import slimeknights.tconstruct.plugin.jei.transfer.CraftingStationTransferInfo;
import slimeknights.tconstruct.plugin.jei.transfer.TinkerStationTransferInfo;
import slimeknights.tconstruct.shared.TinkerMaterials;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.client.screen.HeatingStructureScreen;
import slimeknights.tconstruct.smeltery.client.screen.IScreenWithFluidTank;
import slimeknights.tconstruct.smeltery.client.screen.MelterScreen;
import slimeknights.tconstruct.smeltery.data.SmelteryCompat;
import slimeknights.tconstruct.smeltery.item.CopperCanItem;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.TinkerTools;
import slimeknights.tconstruct.tools.item.ArmorSlotType;
import slimeknights.tconstruct.tools.item.CreativeSlotItem;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@JeiPlugin
public class JEIPlugin implements IModPlugin {
  @Override
  public ResourceLocation getPluginUid() {
    return TConstructJEIConstants.PLUGIN;
  }

  @Override
  public void registerCategories(IRecipeCategoryRegistration registry) {
    final IGuiHelper guiHelper = registry.getJeiHelpers().getGuiHelper();
    // casting
    registry.addRecipeCategories(new CastingBasinCategory(guiHelper));
    registry.addRecipeCategories(new CastingTableCategory(guiHelper));
    registry.addRecipeCategories(new MoldingRecipeCategory(guiHelper));
    // melting and casting
    registry.addRecipeCategories(new MeltingCategory(guiHelper));
    registry.addRecipeCategories(new AlloyRecipeCategory(guiHelper));
    registry.addRecipeCategories(new EntityMeltingRecipeCategory(guiHelper));
    registry.addRecipeCategories(new FoundryCategory(guiHelper));
    // tinker station
    registry.addRecipeCategories(new ModifierRecipeCategory(guiHelper));
    registry.addRecipeCategories(new SeveringCategory(guiHelper));
    // part builder
    registry.addRecipeCategories(new PartBuilderCategory(guiHelper));
  }

  @Override
  public void registerIngredients(IModIngredientRegistration registration) {
    assert Minecraft.getInstance().level != null;
    RecipeManager manager = Minecraft.getInstance().level.getRecipeManager();
    List<ModifierEntry> modifiers = Collections.emptyList();
    if (Config.CLIENT.showModifiersInJEI.get()) {
      modifiers = RecipeHelper.getJEIRecipes(manager, RecipeTypes.TINKER_STATION, IDisplayModifierRecipe.class)
                              .stream()
                              .map(recipe -> recipe.getDisplayResult().getModifier())
                              .distinct()
                              .sorted(Comparator.comparing(Modifier::getId))
                              .map(mod -> new ModifierEntry(mod, 1))
                              .collect(Collectors.toList());
    }

    registration.register(TConstructJEIConstants.ENTITY_TYPE, Collections.emptyList(), new EntityIngredientHelper(), new EntityIngredientRenderer(16));
    registration.register(TConstructJEIConstants.MODIFIER_TYPE, modifiers, new ModifierIngredientHelper(), ModifierBookmarkIngredientRenderer.INSTANCE);
    registration.register(TConstructJEIConstants.PATTERN_TYPE, Collections.emptyList(), new PatternIngredientHelper(), PatternIngredientRenderer.INSTANCE);
  }

  @Override
  public void registerRecipes(IRecipeRegistration register) {
    assert Minecraft.getInstance().level != null;
    RecipeManager manager = Minecraft.getInstance().level.getRecipeManager();
    // casting
    List<IDisplayableCastingRecipe> castingBasinRecipes = RecipeHelper.getJEIRecipes(manager, RecipeTypes.CASTING_BASIN, IDisplayableCastingRecipe.class);
    register.addRecipes(castingBasinRecipes, TConstructJEIConstants.CASTING_BASIN);
    List<IDisplayableCastingRecipe> castingTableRecipes = RecipeHelper.getJEIRecipes(manager, RecipeTypes.CASTING_TABLE, IDisplayableCastingRecipe.class);
    register.addRecipes(castingTableRecipes, TConstructJEIConstants.CASTING_TABLE);

    // melting
    List<MeltingRecipe> meltingRecipes = RecipeHelper.getJEIRecipes(manager, RecipeTypes.MELTING, MeltingRecipe.class);
    register.addRecipes(meltingRecipes, TConstructJEIConstants.MELTING);
    register.addRecipes(meltingRecipes, TConstructJEIConstants.FOUNDRY);
    MeltingFuelHandler.setMeltngFuels(RecipeHelper.getRecipes(manager, RecipeTypes.FUEL, MeltingFuel.class));

    // entity melting
    List<EntityMeltingRecipe> entityMeltingRecipes = RecipeHelper.getJEIRecipes(manager, RecipeTypes.ENTITY_MELTING, EntityMeltingRecipe.class);
    // generate a "default" recipe for all other entity types
    entityMeltingRecipes.add(new DefaultEntityMeltingRecipe(entityMeltingRecipes));
    register.addRecipes(entityMeltingRecipes, TConstructJEIConstants.ENTITY_MELTING);

    // alloying
    List<AlloyRecipe> alloyRecipes = RecipeHelper.getJEIRecipes(manager, RecipeTypes.ALLOYING, AlloyRecipe.class);
    register.addRecipes(alloyRecipes, TConstructJEIConstants.ALLOY);

    // molding
    List<MoldingRecipe> moldingRecipes = ImmutableList.<MoldingRecipe>builder()
      .addAll(RecipeHelper.getJEIRecipes(manager, RecipeTypes.MOLDING_TABLE, MoldingRecipe.class))
      .addAll(RecipeHelper.getJEIRecipes(manager, RecipeTypes.MOLDING_BASIN, MoldingRecipe.class))
      .build();
    register.addRecipes(moldingRecipes, TConstructJEIConstants.MOLDING);

    // modifiers
    List<IDisplayModifierRecipe> modifierRecipes = RecipeHelper.getJEIRecipes(manager, RecipeTypes.TINKER_STATION, IDisplayModifierRecipe.class)
                                                               .stream()
                                                               .sorted((r1, r2) -> {
                                                                 SlotType t1 = r1.getSlotType();
                                                                 SlotType t2 = r2.getSlotType();
                                                                 String n1 = t1 == null ? "zzzzzzzzzz" : t1.getName();
                                                                 String n2 = t2 == null ? "zzzzzzzzzz" : t2.getName();
                                                                 return n1.compareTo(n2);
                                                               }).collect(Collectors.toList());
    register.addRecipes(modifierRecipes, TConstructJEIConstants.MODIFIERS);

    // beheading
    List<SeveringRecipe> severingRecipes = RecipeHelper.getJEIRecipes(manager, RecipeTypes.SEVERING, SeveringRecipe.class);
    register.addRecipes(severingRecipes, TConstructJEIConstants.SEVERING);

    // part builder
    List<MaterialRecipe> materialRecipes = RecipeHelper.getRecipes(manager, RecipeTypes.MATERIAL, MaterialRecipe.class);
    MaterialItemList.setRecipes(materialRecipes);
    List<IDisplayPartBuilderRecipe> partRecipes = RecipeHelper.getJEIRecipes(manager, RecipeTypes.PART_BUILDER, IDisplayPartBuilderRecipe.class);
    register.addRecipes(partRecipes, TConstructJEIConstants.PART_BUILDER);
  }

  /**
   * Adds an item as a casting catalyst, and as a molding catalyst if it has molding recipes
   * @param registry     Catalyst regisry
   * @param item         Item to add
   * @param ownCategory  Category to always add
   * @param type         Molding recipe type
   */
  private static <T extends Recipe<C>, C extends Container> void addCastingCatalyst(IRecipeCatalystRegistration registry, ItemLike item, ResourceLocation ownCategory, RecipeType<T> type) {
    ItemStack stack = new ItemStack(item);
    registry.addRecipeCatalyst(stack, ownCategory);
    assert Minecraft.getInstance().level != null;
    if (!Minecraft.getInstance().level.getRecipeManager().byType(type).isEmpty()) {
      registry.addRecipeCatalyst(stack, TConstructJEIConstants.MOLDING);
    }
  }

  @Override
  public void registerRecipeCatalysts(IRecipeCatalystRegistration registry) {
    // tables
    registry.addRecipeCatalyst(new ItemStack(TinkerTables.partBuilder), TConstructJEIConstants.PART_BUILDER);
    registry.addRecipeCatalyst(new ItemStack(TinkerTables.tinkerStation), TConstructJEIConstants.MODIFIERS);
    registry.addRecipeCatalyst(new ItemStack(TinkerTables.tinkersAnvil), TConstructJEIConstants.MODIFIERS);
    registry.addRecipeCatalyst(new ItemStack(TinkerTables.scorchedAnvil), TConstructJEIConstants.MODIFIERS);

    // smeltery
    registry.addRecipeCatalyst(new ItemStack(TinkerSmeltery.searedMelter), TConstructJEIConstants.MELTING);
    registry.addRecipeCatalyst(new ItemStack(TinkerSmeltery.searedHeater), VanillaRecipeCategoryUid.FUEL);
    addCastingCatalyst(registry, TinkerSmeltery.searedTable, TConstructJEIConstants.CASTING_TABLE, RecipeTypes.MOLDING_TABLE);
    addCastingCatalyst(registry, TinkerSmeltery.searedBasin, TConstructJEIConstants.CASTING_BASIN, RecipeTypes.MOLDING_BASIN);
    registry.addRecipeCatalyst(new ItemStack(TinkerSmeltery.smelteryController), TConstructJEIConstants.MELTING, TConstructJEIConstants.ALLOY, TConstructJEIConstants.ENTITY_MELTING);

    // foundry
    registry.addRecipeCatalyst(new ItemStack(TinkerSmeltery.scorchedAlloyer), TConstructJEIConstants.ALLOY);
    addCastingCatalyst(registry, TinkerSmeltery.scorchedTable, TConstructJEIConstants.CASTING_TABLE, RecipeTypes.MOLDING_TABLE);
    addCastingCatalyst(registry, TinkerSmeltery.scorchedBasin, TConstructJEIConstants.CASTING_BASIN, RecipeTypes.MOLDING_BASIN);
    registry.addRecipeCatalyst(new ItemStack(TinkerSmeltery.foundryController), TConstructJEIConstants.FOUNDRY);

    // modifiers
    for (Item item : TinkerTags.Items.MELEE.getValues()) {
      registry.addRecipeCatalyst(IModifiableDisplay.getDisplayStack(item), TConstructJEIConstants.SEVERING);
    }
  }

  @Override
  public void registerItemSubtypes(ISubtypeRegistration registry) {
    // retexturable blocks
    IIngredientSubtypeInterpreter<ItemStack> tables = (stack, context) -> {
      if (context == UidContext.Ingredient) {
        return RetexturedBlockItem.getTextureName(stack);
      }
      return IIngredientSubtypeInterpreter.NONE;
    };
    registry.registerSubtypeInterpreter(TinkerTables.craftingStation.asItem(), tables);
    registry.registerSubtypeInterpreter(TinkerTables.partBuilder.asItem(), tables);
    registry.registerSubtypeInterpreter(TinkerTables.tinkerStation.asItem(), tables);
    registry.registerSubtypeInterpreter(TinkerTables.tinkersAnvil.asItem(), tables);
    registry.registerSubtypeInterpreter(TinkerTables.scorchedAnvil.asItem(), tables);

    IIngredientSubtypeInterpreter<ItemStack> toolPartInterpreter = (stack, context) -> {
      MaterialVariantId materialId = IMaterialItem.getMaterialFromStack(stack);
      if (materialId.equals(IMaterial.UNKNOWN_ID)) {
        return IIngredientSubtypeInterpreter.NONE;
      }
      return materialId.getId().toString();
    };

    // parts
    for (Item item : TinkerTags.Items.TOOL_PARTS.getValues()) {
      registry.registerSubtypeInterpreter(item, toolPartInterpreter);
    }

    // tools
    Item slimeskull = TinkerTools.slimesuit.get(ArmorSlotType.HELMET);
    registry.registerSubtypeInterpreter(slimeskull, ToolSubtypeInterpreter.ALWAYS);
    for (Item item : TinkerTags.Items.MULTIPART_TOOL.getValues()) {
      if (item != slimeskull) {
        registry.registerSubtypeInterpreter(item, ToolSubtypeInterpreter.INGREDIENT);
      }
    }

    registry.registerSubtypeInterpreter(TinkerSmeltery.copperCan.get(), (stack, context) -> CopperCanItem.getSubtype(stack));
    registry.registerSubtypeInterpreter(TinkerModifiers.creativeSlotItem.get(), (stack, context) -> {
      SlotType slotType = CreativeSlotItem.getSlot(stack);
      return slotType != null ? slotType.getName() : "";
    });
  }

  @Override
  public void registerGuiHandlers(IGuiHandlerRegistration registration) {
    registration.addGenericGuiContainerHandler(MelterScreen.class, new GuiContainerTankHandler<>());
    registration.addGenericGuiContainerHandler(HeatingStructureScreen.class, new GuiContainerTankHandler<>());
  }

  @Override
  public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
    registration.addRecipeTransferHandler(new CraftingStationTransferInfo());
    registration.addRecipeTransferHandler(new TinkerStationTransferInfo());
  }

  /**
   * Removes a fluid from JEI
   * @param manager  Manager
   * @param fluid    Fluid to remove
   * @param bucket   Fluid bucket to remove
   */
  private static void removeFluid(IIngredientManager manager, Fluid fluid, Item bucket) {
    manager.removeIngredientsAtRuntime(VanillaTypes.FLUID, Collections.singleton(new FluidStack(fluid, FluidAttributes.BUCKET_VOLUME)));
    manager.removeIngredientsAtRuntime(VanillaTypes.ITEM, Collections.singleton(new ItemStack(bucket)));
  }

  /**
   * Hides an item if the related tag is empty
   * @param manager  Ingredient manager
   * @param item     Cast instance
   * @param tagName  Tag to check
   */
  @SuppressWarnings("SameParameterValue")
  private static void optionalItem(IIngredientManager manager, ItemLike item, String tagName) {
    Tag<Item> tag = SerializationTags.getInstance().getOrEmpty(Registry.ITEM_REGISTRY).getTag(new ResourceLocation("forge", tagName));
    if (tag == null || tag.getValues().isEmpty()) {
      manager.removeIngredientsAtRuntime(VanillaTypes.ITEM, Collections.singletonList(new ItemStack(item)));
    }
  }

  /**
   * Hides casts if the related tag is empty
   * @param manager  Ingredient manager
   * @param cast     Cast instance
   */
  private static void optionalCast(IIngredientManager manager, CastItemObject cast) {
    Tag<Item> tag = SerializationTags.getInstance().getOrEmpty(Registry.ITEM_REGISTRY).getTag(new ResourceLocation("forge", cast.getName().getPath() + "s"));
    if (tag == null || tag.getValues().isEmpty()) {
      manager.removeIngredientsAtRuntime(VanillaTypes.ITEM, cast.values().stream().map(ItemStack::new).collect(Collectors.toList()));
    }
  }

  @Override
  public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
    IIngredientManager manager = jeiRuntime.getIngredientManager();

    // hide knightslime and slimesteel until implemented
    removeFluid(manager, TinkerFluids.moltenSoulsteel.get(), TinkerFluids.moltenSoulsteel.asItem());
    removeFluid(manager, TinkerFluids.moltenKnightslime.get(), TinkerFluids.moltenKnightslime.asItem());
    // hide compat that is not present
    for (SmelteryCompat compat : SmelteryCompat.values()) {
      Tag<Item> ingot = SerializationTags.getInstance().getOrEmpty(Registry.ITEM_REGISTRY).getTag(new ResourceLocation("forge", "ingots/" + compat.getName()));
      if (ingot == null || ingot.getValues().isEmpty()) {
        removeFluid(manager, compat.getFluid().get(), compat.getBucket());
      }
    }
    if (!ModList.get().isLoaded("ceramics")) {
      removeFluid(manager, TinkerFluids.moltenPorcelain.get(), TinkerFluids.moltenPorcelain.asItem());
    }
    optionalCast(manager, TinkerSmeltery.plateCast);
    optionalCast(manager, TinkerSmeltery.gearCast);
    optionalCast(manager, TinkerSmeltery.coinCast);
    optionalCast(manager, TinkerSmeltery.wireCast);
    optionalItem(manager, TinkerMaterials.necroniumBone, "ingots/uranium");
  }

  /** Class to pass {@link IScreenWithFluidTank} into JEI */
  public static class GuiContainerTankHandler<C extends AbstractContainerMenu, T extends AbstractContainerScreen<C> & IScreenWithFluidTank> implements IGuiContainerHandler<T> {
    @Override
    @Nullable
    public Object getIngredientUnderMouse(T containerScreen, double mouseX, double mouseY) {
      return containerScreen.getIngredientUnderMouse(mouseX, mouseY);
    }
  }

  /** Subtype interpreter for tools, treats the tool as unique in ingredient list, generic in recipes */
  public enum ToolSubtypeInterpreter implements IIngredientSubtypeInterpreter<ItemStack> {
    ALWAYS, INGREDIENT;

    @Override
    public String apply(ItemStack itemStack, UidContext context) {
      if (this == ALWAYS || context == UidContext.Ingredient) {
        StringBuilder builder = new StringBuilder();
        List<MaterialVariantId> materialList = MaterialIdNBT.from(itemStack).getMaterials();
        if (!materialList.isEmpty()) {
          // append first entry without a comma
          builder.append(materialList.get(0));
          for (int i = 1; i < materialList.size(); i++) {
            builder.append(',');
            builder.append(materialList.get(i).getId());
          }
        }
        return builder.toString();
      }
      return NONE;
    }
  }
}
