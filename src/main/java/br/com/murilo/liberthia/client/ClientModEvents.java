package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.client.renderer.BlackHoleRenderer;
import br.com.murilo.liberthia.client.screen.*;
import br.com.murilo.liberthia.registry.ModEntities;
import br.com.murilo.liberthia.registry.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import br.com.murilo.liberthia.registry.ModBlocks;

@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // r120: Void Larva (silverfish reskin) + Mini Black Hole
        event.registerEntityRenderer(ModEntities.VOID_LARVA.get(),
                br.com.murilo.liberthia.magic.spell.voidspell.VoidLarvaRenderer::new);
        event.registerEntityRenderer(ModEntities.MINI_BLACK_HOLE.get(),
                br.com.murilo.liberthia.magic.spell.voidspell.MiniBlackHoleRenderer::new);
        // r165: Void Tentacle — billboard sprite renderer com flipbook animation
        event.registerEntityRenderer(ModEntities.VOID_TENTACLE.get(),
                br.com.murilo.liberthia.magic.spell.voidspell.VoidTentacleRenderer::new);
        // r165: Void Effect — generic sprite-animation renderer (5 types)
        event.registerEntityRenderer(ModEntities.VOID_EFFECT.get(),
                br.com.murilo.liberthia.magic.spell.voidspell.VoidEffectRenderer::new);
        // r166: Sprite VFX — registry-based renderer (20 effect types, 1288 frames)
        event.registerEntityRenderer(ModEntities.SPRITE_VFX.get(),
                br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRenderer::new);
        event.registerEntityRenderer(ModEntities.BLACK_HOLE.get(), BlackHoleRenderer::new);
        // r178: Manifestação do Vazio (sombra + olhos vermelhos girando + tentáculos)
        event.registerEntityRenderer(ModEntities.VOID_MANIFESTATION.get(),
                br.com.murilo.liberthia.client.renderer.VoidManifestationRenderer::new);
        // r135: Drygmy familiar (passive farm helper)
        event.registerEntityRenderer(ModEntities.DRYGMY.get(),
                br.com.murilo.liberthia.magic.familiar.DrygmyRenderer::new);
        event.registerEntityRenderer(ModEntities.DARK_MATTER_SPORE.get(), br.com.murilo.liberthia.client.renderer.DarkMatterSporeRenderer::new);
        // REMOVIDO v0.1.13: CLEANSING_GRENADE renderer
        event.registerEntityRenderer(ModEntities.CORRUPTED_ZOMBIE.get(), br.com.murilo.liberthia.client.renderer.CorruptedZombieRenderer::new);
        event.registerEntityRenderer(ModEntities.SPORE_SPITTER.get(), br.com.murilo.liberthia.client.renderer.SporeSpitterRenderer::new);
        event.registerEntityRenderer(ModEntities.WHITE_MATTER_EXPLOSION.get(), br.com.murilo.liberthia.client.renderer.WhiteMatterExplosionRenderer::new);
        event.registerEntityRenderer(ModEntities.CLONE_PLAYER.get(), br.com.murilo.liberthia.client.renderer.ClonePlayerRenderer::new);
        // r48: Reflection Entity (clone com AI) — usa renderer próprio
        event.registerEntityRenderer(ModEntities.REFLECTION_ENTITY.get(),
                br.com.murilo.liberthia.cosmic.observatory.client.ReflectionEntityRenderer::new);
        // r64: Observation Projectile — renderer mínimo (trail via particles em tick())
        event.registerEntityRenderer(ModEntities.OBSERVATION_PROJECTILE.get(),
                br.com.murilo.liberthia.observation.entity.EntityObservationProjectileRenderer::new);
        // r24: SoulBody reusa o ClonePlayerRenderer (mesma skin via OWNER_UUID)
        event.registerEntityRenderer(ModEntities.SOUL_BODY.get(), br.com.murilo.liberthia.client.renderer.ClonePlayerRenderer::new);
        event.registerEntityRenderer(ModEntities.DARK_CONSCIOUSNESS.get(), br.com.murilo.liberthia.client.renderer.DarkConsciousnessRenderer::new);
        event.registerEntityRenderer(ModEntities.EYE_OF_HORUS.get(), br.com.murilo.liberthia.client.renderer.EyeOfHorusRenderer::new);
        event.registerEntityRenderer(ModEntities.LURKER.get(), br.com.murilo.liberthia.cosmic.lurker.LurkerRenderer::new);
        // Reuse vanilla Silverfish renderer as fast, stable base for BloodWorm
        event.registerEntityRenderer(ModEntities.BLOOD_WORM.get(),
                ctx -> new br.com.murilo.liberthia.client.renderer.BloodWormRenderer(ctx, "blood_worm"));
        event.registerEntityRenderer(ModEntities.FLESH_CRAWLER.get(),
                ctx -> new br.com.murilo.liberthia.client.renderer.BloodWormRenderer(ctx, "flesh_crawler"));
        event.registerEntityRenderer(ModEntities.GORE_WORM.get(),
                ctx -> new br.com.murilo.liberthia.client.renderer.BloodWormRenderer(ctx, "gore_worm"));
        // Blood Orb is particle-only; render as invisible using a no-op renderer
        event.registerEntityRenderer(ModEntities.BLOOD_CULTIST.get(),
                br.com.murilo.liberthia.client.renderer.BloodCultistRenderer::new);
        event.registerEntityRenderer(ModEntities.BLOOD_PRIEST.get(),
                br.com.murilo.liberthia.client.renderer.BloodPriestRenderer::new);
        event.registerEntityRenderer(ModEntities.WOUNDED_PILGRIM.get(),
                br.com.murilo.liberthia.client.renderer.WoundedPilgrimRenderer::new);
        event.registerEntityRenderer(ModEntities.HEMO_BOLT.get(),
                br.com.murilo.liberthia.client.renderer.HemoBoltRenderer::new);
        event.registerEntityRenderer(ModEntities.BLEEDING_ARROW.get(),
                net.minecraft.client.renderer.entity.TippableArrowRenderer::new);
        event.registerEntityRenderer(ModEntities.ORDER_PALADIN.get(),
                br.com.murilo.liberthia.client.renderer.OrderPaladinRenderer::new);
        event.registerEntityRenderer(ModEntities.BLOOD_PEARL.get(),
                ctx -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx, 1.0F, true));
        event.registerEntityRenderer(ModEntities.VEILING_ORB.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.MIND_SPLINTER_DART.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.LIGHTNING_GRENADE.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.BURNING_GEM.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.FROST_FLASK.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.PURIFYING_FLASK.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        // r113: SpellProjectileEntity — renderer 3D AAA com 3 camadas billboarded
        // (outer aura + inner core + hot white center) + spin + pulse, tinted
        // por SpellSchool. Combinado com o trail de spritesheet animado.
        event.registerEntityRenderer(ModEntities.SPELL_PROJECTILE.get(),
                br.com.murilo.liberthia.magic.spell.client.SpellProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.POSSESSED_ZOMBIE.get(),
                net.minecraft.client.renderer.entity.ZombieRenderer::new);
        event.registerEntityRenderer(ModEntities.POSSESSED_SKELETON.get(),
                net.minecraft.client.renderer.entity.SkeletonRenderer::new);

        // BlockEntity renderers
        event.registerBlockEntityRenderer(
                br.com.murilo.liberthia.registry.ModBlockEntities.LASER_EMITTER.get(),
                br.com.murilo.liberthia.client.renderer.LaserBeamRenderer::new);

        // v0.1.39: Matter Tank BER — renderiza fluido visual proporcional ao
        // fillage, substitui o sistema antigo de blockstate level 0-4 que o
        // user reclamou que "mudava de modelo".
        event.registerBlockEntityRenderer(
                br.com.murilo.liberthia.registry.ModBlockEntities.MATTER_TANK.get(),
                br.com.murilo.liberthia.client.renderer.MatterTankRenderer::new);

        // r173: Potion Jar BER — líquido (Source roxo) animado enchendo por dentro.
        event.registerBlockEntityRenderer(
                br.com.murilo.liberthia.registry.ModBlockEntities.POTION_JAR.get(),
                br.com.murilo.liberthia.client.renderer.PotionJarRenderer::new);



        // --- Blood Warden boss: ZOMBIE layer (has 'hat') + Husk texture, scaled 1.6× ---
        event.registerEntityRenderer(ModEntities.BLOOD_WARDEN.get(), ctx -> {
            net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.entity.BloodWardenBossEntity> body =
                    new net.minecraft.client.model.HumanoidModel<>(
                            ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.ZOMBIE));
            return new net.minecraft.client.renderer.entity.MobRenderer<
                    br.com.murilo.liberthia.entity.BloodWardenBossEntity,
                    net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.entity.BloodWardenBossEntity>>(ctx, body, 1.0F) {
                @Override
                public net.minecraft.resources.ResourceLocation getTextureLocation(
                        br.com.murilo.liberthia.entity.BloodWardenBossEntity e) {
                    return net.minecraft.resources.ResourceLocation.tryBuild("minecraft", "textures/entity/zombie/husk.png");
                }
                @Override
                protected void scale(br.com.murilo.liberthia.entity.BloodWardenBossEntity entity,
                                     com.mojang.blaze3d.vertex.PoseStack pose, float partialTick) {
                    pose.scale(1.6F, 1.6F, 1.6F);
                }
            };
        });

        // --- Weaving Shade: ZOMBIE layer + Drowned texture, scaled 0.6× ---
        event.registerEntityRenderer(ModEntities.WEAVING_SHADE.get(), ctx -> {
            net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.entity.WeavingShadeEntity> body =
                    new net.minecraft.client.model.HumanoidModel<>(
                            ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.ZOMBIE));
            return new net.minecraft.client.renderer.entity.MobRenderer<
                    br.com.murilo.liberthia.entity.WeavingShadeEntity,
                    net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.entity.WeavingShadeEntity>>(ctx, body, 0.3F) {
                @Override
                public net.minecraft.resources.ResourceLocation getTextureLocation(
                        br.com.murilo.liberthia.entity.WeavingShadeEntity e) {
                    return net.minecraft.resources.ResourceLocation.tryBuild("minecraft", "textures/entity/zombie/drowned.png");
                }
                @Override
                protected void scale(br.com.murilo.liberthia.entity.WeavingShadeEntity entity,
                                     com.mojang.blaze3d.vertex.PoseStack pose, float partialTick) {
                    pose.scale(0.6F, 0.6F, 0.6F);
                }
            };
        });

        // --- Disarmer: ZOMBIE layer + zombie texture ---
        event.registerEntityRenderer(ModEntities.DISARMER.get(), ctx -> {
            net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.entity.DisarmerEntity> body =
                    new net.minecraft.client.model.HumanoidModel<>(
                            ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.ZOMBIE));
            return new net.minecraft.client.renderer.entity.MobRenderer<
                    br.com.murilo.liberthia.entity.DisarmerEntity,
                    net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.entity.DisarmerEntity>>(ctx, body, 0.5F) {
                @Override
                public net.minecraft.resources.ResourceLocation getTextureLocation(
                        br.com.murilo.liberthia.entity.DisarmerEntity e) {
                    return net.minecraft.resources.ResourceLocation.tryBuild("minecraft", "textures/entity/zombie/zombie.png");
                }
            };
        });
        event.registerEntityRenderer(ModEntities.BLOOD_MAGE.get(),
                br.com.murilo.liberthia.client.renderer.BloodMageRenderer::new);
        event.registerEntityRenderer(ModEntities.BLOOD_HOUND.get(),
                br.com.murilo.liberthia.client.renderer.BloodHoundRenderer::new);
        event.registerEntityRenderer(ModEntities.FLESH_MOTHER_BOSS.get(),
                br.com.murilo.liberthia.client.renderer.FleshMotherBossRenderer::new);
        event.registerEntityRenderer(ModEntities.WEAVING_SHADE.get(),
                br.com.murilo.liberthia.client.renderer.WeavingShadeRenderer::new);
        event.registerEntityRenderer(ModEntities.BLOOD_WARDEN.get(),
                br.com.murilo.liberthia.client.renderer.BloodWardenRenderer::new);
        event.registerEntityRenderer(ModEntities.DISARMER.get(),
                br.com.murilo.liberthia.client.renderer.DisarmerRenderer::new);
        event.registerEntityRenderer(ModEntities.BLOOD_ORB.get(),
                ctx -> new net.minecraft.client.renderer.entity.EntityRenderer<br.com.murilo.liberthia.entity.BloodOrbEntity>(ctx) {
                    @Override public net.minecraft.resources.ResourceLocation getTextureLocation(br.com.murilo.liberthia.entity.BloodOrbEntity e) {
                        return new net.minecraft.resources.ResourceLocation("textures/misc/white.png");
                    }
                    @Override public boolean shouldRender(br.com.murilo.liberthia.entity.BloodOrbEntity e, net.minecraft.client.renderer.culling.Frustum f, double x, double y, double z) { return false; }
                });

        // r81: Horror Framework entities — usam HumanoidMobRenderer simples
        // (Monster genérico) com modelo humanoide básico
        event.registerEntityRenderer(ModEntities.EMPTY_MAN.get(),
                ctx -> new net.minecraft.client.renderer.entity.HumanoidMobRenderer<br.com.murilo.liberthia.cosmic.horror.entity.EmptyManEntity, net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.cosmic.horror.entity.EmptyManEntity>>(
                        ctx,
                        new net.minecraft.client.model.HumanoidModel<>(
                                ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER)),
                        0.5F) {
                    @Override
                    public net.minecraft.resources.ResourceLocation getTextureLocation(br.com.murilo.liberthia.cosmic.horror.entity.EmptyManEntity e) {
                        return new net.minecraft.resources.ResourceLocation("textures/entity/zombie/zombie.png");
                    }
                });
        // r145: Observer agora usa textura de madeira customizada (totem-like)
        // — antes usava textura do Warden em modelo de Zombie (UV mismatch = textura bugada)
        event.registerEntityRenderer(ModEntities.OBSERVER.get(),
                ctx -> new net.minecraft.client.renderer.entity.HumanoidMobRenderer<br.com.murilo.liberthia.cosmic.horror.entity.ObserverEntity, net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.cosmic.horror.entity.ObserverEntity>>(
                        ctx,
                        new net.minecraft.client.model.HumanoidModel<>(
                                ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER)),
                        0.5F) {
                    @Override
                    public net.minecraft.resources.ResourceLocation getTextureLocation(br.com.murilo.liberthia.cosmic.horror.entity.ObserverEntity e) {
                        return new net.minecraft.resources.ResourceLocation("liberthia", "textures/entity/observer.png");
                    }
                });
        // O Visitante + A Mulher do Horizonte — humanoides (modelo PLAYER), textura vanilla
        event.registerEntityRenderer(ModEntities.VISITANTE.get(),
                ctx -> new net.minecraft.client.renderer.entity.HumanoidMobRenderer<br.com.murilo.liberthia.cosmic.horror.entity.VisitanteEntity, net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.cosmic.horror.entity.VisitanteEntity>>(
                        ctx,
                        new net.minecraft.client.model.HumanoidModel<>(
                                ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER)),
                        0.5F) {
                    @Override
                    public net.minecraft.resources.ResourceLocation getTextureLocation(br.com.murilo.liberthia.cosmic.horror.entity.VisitanteEntity e) {
                        return new net.minecraft.resources.ResourceLocation("textures/entity/zombie/zombie.png");
                    }
                });
        event.registerEntityRenderer(ModEntities.MULHER_HORIZONTE.get(),
                ctx -> new net.minecraft.client.renderer.entity.HumanoidMobRenderer<br.com.murilo.liberthia.cosmic.horror.entity.MulherDoHorizonteEntity, net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.cosmic.horror.entity.MulherDoHorizonteEntity>>(
                        ctx,
                        new net.minecraft.client.model.HumanoidModel<>(
                                ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER)),
                        0.5F) {
                    @Override
                    public net.minecraft.resources.ResourceLocation getTextureLocation(br.com.murilo.liberthia.cosmic.horror.entity.MulherDoHorizonteEntity e) {
                        return new net.minecraft.resources.ResourceLocation("textures/entity/zombie/zombie.png");
                    }
                });
        // r178: O Ídolo — humanoide pálido alto, textura própria
        event.registerEntityRenderer(ModEntities.IDOL.get(),
                ctx -> new net.minecraft.client.renderer.entity.HumanoidMobRenderer<br.com.murilo.liberthia.cosmic.idol.IdolEntity, net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.cosmic.idol.IdolEntity>>(
                        ctx,
                        new net.minecraft.client.model.HumanoidModel<>(
                                ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER)),
                        0.5F) {
                    @Override
                    public net.minecraft.resources.ResourceLocation getTextureLocation(br.com.murilo.liberthia.cosmic.idol.IdolEntity en) {
                        return new net.minecraft.resources.ResourceLocation(
                                br.com.murilo.liberthia.LiberthiaMod.MODID, "textures/entity/idol.png");
                    }
                });
        // r178: O Sem-Rosto (textura pálida do Ídolo) + O do Teto (zombie vanilla)
        event.registerEntityRenderer(ModEntities.FACELESS.get(),
                ctx -> new net.minecraft.client.renderer.entity.HumanoidMobRenderer<br.com.murilo.liberthia.cosmic.horror.entity.FacelessEntity, net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.cosmic.horror.entity.FacelessEntity>>(
                        ctx, new net.minecraft.client.model.HumanoidModel<>(ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER)), 0.5F) {
                    @Override
                    public net.minecraft.resources.ResourceLocation getTextureLocation(br.com.murilo.liberthia.cosmic.horror.entity.FacelessEntity en) {
                        return new net.minecraft.resources.ResourceLocation(br.com.murilo.liberthia.LiberthiaMod.MODID, "textures/entity/idol.png");
                    }
                });
        event.registerEntityRenderer(ModEntities.CEILING_LURKER.get(),
                ctx -> new net.minecraft.client.renderer.entity.HumanoidMobRenderer<br.com.murilo.liberthia.cosmic.horror.entity.CeilingLurkerEntity, net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.cosmic.horror.entity.CeilingLurkerEntity>>(
                        ctx, new net.minecraft.client.model.HumanoidModel<>(ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER)), 0.5F) {
                    @Override
                    public net.minecraft.resources.ResourceLocation getTextureLocation(br.com.murilo.liberthia.cosmic.horror.entity.CeilingLurkerEntity en) {
                        return new net.minecraft.resources.ResourceLocation("textures/entity/zombie/zombie.png");
                    }
                });
        // r178: O Coletor de Olhos (husk vanilla) + O Vizinho (zombie aldeão vanilla)
        event.registerEntityRenderer(ModEntities.EYE_COLLECTOR.get(),
                ctx -> new net.minecraft.client.renderer.entity.HumanoidMobRenderer<br.com.murilo.liberthia.cosmic.horror.entity.EyeCollectorEntity, net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.cosmic.horror.entity.EyeCollectorEntity>>(
                        ctx, new net.minecraft.client.model.HumanoidModel<>(ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER)), 0.5F) {
                    @Override
                    public net.minecraft.resources.ResourceLocation getTextureLocation(br.com.murilo.liberthia.cosmic.horror.entity.EyeCollectorEntity en) {
                        return new net.minecraft.resources.ResourceLocation("textures/entity/zombie/husk.png");
                    }
                });
        event.registerEntityRenderer(ModEntities.NEIGHBOR.get(),
                ctx -> new net.minecraft.client.renderer.entity.HumanoidMobRenderer<br.com.murilo.liberthia.cosmic.horror.entity.NeighborEntity, net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.cosmic.horror.entity.NeighborEntity>>(
                        ctx, new net.minecraft.client.model.HumanoidModel<>(ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER)), 0.5F) {
                    @Override
                    public net.minecraft.resources.ResourceLocation getTextureLocation(br.com.murilo.liberthia.cosmic.horror.entity.NeighborEntity en) {
                        return new net.minecraft.resources.ResourceLocation("textures/entity/zombie_villager/zombie_villager.png");
                    }
                });
        // O Caçador — piloto SmartBrainLib (humanoide, textura zombie vanilla)
        event.registerEntityRenderer(ModEntities.CACADOR.get(),
                ctx -> new net.minecraft.client.renderer.entity.HumanoidMobRenderer<br.com.murilo.liberthia.cosmic.horror.entity.CacadorEntity, net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.cosmic.horror.entity.CacadorEntity>>(
                        ctx,
                        new net.minecraft.client.model.HumanoidModel<>(
                                ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER)),
                        0.5F) {
                    @Override
                    public net.minecraft.resources.ResourceLocation getTextureLocation(br.com.murilo.liberthia.cosmic.horror.entity.CacadorEntity e) {
                        return new net.minecraft.resources.ResourceLocation("textures/entity/zombie/zombie.png");
                    }
                });
        // O Espreitador — humanoide pálido (textura husk vanilla)
        event.registerEntityRenderer(ModEntities.ESPREITADOR.get(),
                ctx -> new net.minecraft.client.renderer.entity.HumanoidMobRenderer<br.com.murilo.liberthia.cosmic.horror.entity.EspreitadorEntity, net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.cosmic.horror.entity.EspreitadorEntity>>(
                        ctx,
                        new net.minecraft.client.model.HumanoidModel<>(
                                ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER)),
                        0.5F) {
                    @Override
                    public net.minecraft.resources.ResourceLocation getTextureLocation(br.com.murilo.liberthia.cosmic.horror.entity.EspreitadorEntity e) {
                        return new net.minecraft.resources.ResourceLocation("textures/entity/zombie/husk.png");
                    }
                });
        // r150: 8 Wooden Horror variants — cada um aponta pra sua textura
        registerWoodenHorrorRenderer(event, ModEntities.WOODEN_CHARCOAL.get(), "wooden_charcoal");
        registerWoodenHorrorRenderer(event, ModEntities.WOODEN_PALE_OAK.get(), "wooden_pale_oak");
        registerWoodenHorrorRenderer(event, ModEntities.WOODEN_ROTTED_BIRCH.get(), "wooden_rotted_birch");
        registerWoodenHorrorRenderer(event, ModEntities.WOODEN_BLEEDING_MAPLE.get(), "wooden_bleeding_maple");
        registerWoodenHorrorRenderer(event, ModEntities.WOODEN_MOSSY.get(), "wooden_mossy");
        registerWoodenHorrorRenderer(event, ModEntities.WOODEN_FROZEN_PINE.get(), "wooden_frozen_pine");
        registerWoodenHorrorRenderer(event, ModEntities.WOODEN_BURNING_ACACIA.get(), "wooden_burning_acacia");
        registerWoodenHorrorRenderer(event, ModEntities.WOODEN_CURSED_MAHOGANY.get(), "wooden_cursed_mahogany");
        // Absence: renderer no-op (invisível por design)
        event.registerEntityRenderer(ModEntities.ABSENCE.get(),
                ctx -> new net.minecraft.client.renderer.entity.EntityRenderer<br.com.murilo.liberthia.cosmic.horror.entity.AbsenceEntity>(ctx) {
                    @Override public net.minecraft.resources.ResourceLocation getTextureLocation(br.com.murilo.liberthia.cosmic.horror.entity.AbsenceEntity e) {
                        return new net.minecraft.resources.ResourceLocation("textures/misc/white.png");
                    }
                    @Override public boolean shouldRender(br.com.murilo.liberthia.cosmic.horror.entity.AbsenceEntity e, net.minecraft.client.renderer.culling.Frustum f, double x, double y, double z) { return false; }
                });
        // Remembered: usa HumanoidMobRenderer com modelo magro
        event.registerEntityRenderer(ModEntities.REMEMBERED.get(),
                ctx -> new net.minecraft.client.renderer.entity.HumanoidMobRenderer<br.com.murilo.liberthia.cosmic.horror.entity.RememberedEntity, net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.cosmic.horror.entity.RememberedEntity>>(
                        ctx,
                        new net.minecraft.client.model.HumanoidModel<>(
                                ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.SKELETON)),
                        0.5F) {
                    @Override
                    public net.minecraft.resources.ResourceLocation getTextureLocation(br.com.murilo.liberthia.cosmic.horror.entity.RememberedEntity e) {
                        return new net.minecraft.resources.ResourceLocation("textures/entity/skeleton/skeleton.png");
                    }
                });

        // r87/r163: Wizards — HumanoidMobRenderer com texture custom (r163 — antes
        // todos usavam villager.png que dava texture missing pra alguns mobs).
        registerWizardRenderer(event, ModEntities.PYROMANCER.get(),       "liberthia:textures/entity/wizard/pyromancer.png");
        registerWizardRenderer(event, ModEntities.CRYOMANCER.get(),       "liberthia:textures/entity/wizard/cryomancer.png");
        registerWizardRenderer(event, ModEntities.ELECTROMANCER.get(),    "liberthia:textures/entity/wizard/electromancer.png");
        registerWizardRenderer(event, ModEntities.NECROMANCER.get(),      "liberthia:textures/entity/wizard/necromancer.png");
        registerWizardRenderer(event, ModEntities.ELDRITCH_CULTIST.get(), "liberthia:textures/entity/wizard/eldritch_cultist.png");

        // r95: Familiar renderers — minimal renderer (particles via aiStep handle visual)
        event.registerEntityRenderer(ModEntities.WISP_PICKER.get(),
                ctx -> new net.minecraft.client.renderer.entity.EntityRenderer<br.com.murilo.liberthia.magic.familiar.WispPickerEntity>(ctx) {
                    @Override public net.minecraft.resources.ResourceLocation getTextureLocation(br.com.murilo.liberthia.magic.familiar.WispPickerEntity e) {
                        return new net.minecraft.resources.ResourceLocation("textures/misc/white.png");
                    }
                });
        event.registerEntityRenderer(ModEntities.GROVE_SPRITE.get(),
                ctx -> new net.minecraft.client.renderer.entity.EntityRenderer<br.com.murilo.liberthia.magic.familiar.GroveSpriteEntity>(ctx) {
                    @Override public net.minecraft.resources.ResourceLocation getTextureLocation(br.com.murilo.liberthia.magic.familiar.GroveSpriteEntity e) {
                        return new net.minecraft.resources.ResourceLocation("textures/misc/white.png");
                    }
                });
        event.registerEntityRenderer(ModEntities.SOUL_REAPER.get(),
                ctx -> new net.minecraft.client.renderer.entity.EntityRenderer<br.com.murilo.liberthia.magic.familiar.SoulReaperEntity>(ctx) {
                    @Override public net.minecraft.resources.ResourceLocation getTextureLocation(br.com.murilo.liberthia.magic.familiar.SoulReaperEntity e) {
                        return new net.minecraft.resources.ResourceLocation("textures/misc/white.png");
                    }
                });
        // r106: 3 More Wizards
        registerWizardRenderer(event, ModEntities.APOTHECARIST.get(), "liberthia:textures/entity/wizard/apothecarist.png");
        registerWizardRenderer(event, ModEntities.KEEPER.get(), "liberthia:textures/entity/wizard/keeper.png");
        registerWizardRenderer(event, ModEntities.ARCHEVOKER.get(), "liberthia:textures/entity/wizard/archevoker.png");

        // r109: 3 More Familiars (no-op renderers — particles via aiStep)
        event.registerEntityRenderer(ModEntities.WHELP.get(),
                ctx -> new net.minecraft.client.renderer.entity.EntityRenderer<br.com.murilo.liberthia.magic.familiar.WhelpEntity>(ctx) {
                    @Override public net.minecraft.resources.ResourceLocation getTextureLocation(br.com.murilo.liberthia.magic.familiar.WhelpEntity e) {
                        return new net.minecraft.resources.ResourceLocation("textures/misc/white.png");
                    }
                });
        event.registerEntityRenderer(ModEntities.CARBUNCLE.get(),
                ctx -> new net.minecraft.client.renderer.entity.EntityRenderer<br.com.murilo.liberthia.magic.familiar.CarbuncleEntity>(ctx) {
                    @Override public net.minecraft.resources.ResourceLocation getTextureLocation(br.com.murilo.liberthia.magic.familiar.CarbuncleEntity e) {
                        return new net.minecraft.resources.ResourceLocation("textures/misc/white.png");
                    }
                });
        event.registerEntityRenderer(ModEntities.AMETHYST_GOLEM.get(),
                ctx -> new net.minecraft.client.renderer.entity.EntityRenderer<br.com.murilo.liberthia.magic.familiar.AmethystGolemEntity>(ctx) {
                    @Override public net.minecraft.resources.ResourceLocation getTextureLocation(br.com.murilo.liberthia.magic.familiar.AmethystGolemEntity e) {
                        return new net.minecraft.resources.ResourceLocation("textures/misc/white.png");
                    }
                });

        // r164 CRASH FIX: estes 4 entities estavam REGISTERED em ModEntities
        // mas NÃO TINHAM renderer client — quando spawnavam (Cryomancer cast,
        // Abyssal Lich boss summon) o EntityRenderDispatcher pegava null e
        // crashava com NullPointerException no LevelRenderer.
        event.registerEntityRenderer(ModEntities.FROZEN_HUMANOID.get(),
                ctx -> new net.minecraft.client.renderer.entity.HumanoidMobRenderer<br.com.murilo.liberthia.magic.spells.FrozenHumanoidEntity, net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.magic.spells.FrozenHumanoidEntity>>(
                        ctx, new net.minecraft.client.model.HumanoidModel<>(
                                ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER)),
                        0.5F) {
                    @Override
                    public net.minecraft.resources.ResourceLocation getTextureLocation(
                            br.com.murilo.liberthia.magic.spells.FrozenHumanoidEntity e) {
                        return new net.minecraft.resources.ResourceLocation(
                                "minecraft", "textures/entity/skeleton/stray.png");
                    }
                });
        event.registerEntityRenderer(ModEntities.ABYSSAL_LICH.get(),
                ctx -> new net.minecraft.client.renderer.entity.HumanoidMobRenderer<br.com.murilo.liberthia.magic.boss.AbyssalLichEntity, net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.magic.boss.AbyssalLichEntity>>(
                        ctx, new net.minecraft.client.model.HumanoidModel<>(
                                ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER)),
                        0.6F) {
                    @Override
                    public net.minecraft.resources.ResourceLocation getTextureLocation(
                            br.com.murilo.liberthia.magic.boss.AbyssalLichEntity e) {
                        return new net.minecraft.resources.ResourceLocation(
                                "minecraft", "textures/entity/illager/evoker.png");
                    }
                });
        event.registerEntityRenderer(ModEntities.LICH_STALKER.get(),
                ctx -> new net.minecraft.client.renderer.entity.HumanoidMobRenderer<br.com.murilo.liberthia.magic.boss.LichStalkerEntity, net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.magic.boss.LichStalkerEntity>>(
                        ctx, new net.minecraft.client.model.HumanoidModel<>(
                                ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER)),
                        0.5F) {
                    @Override
                    public net.minecraft.resources.ResourceLocation getTextureLocation(
                            br.com.murilo.liberthia.magic.boss.LichStalkerEntity e) {
                        return new net.minecraft.resources.ResourceLocation(
                                "minecraft", "textures/entity/skeleton/wither_skeleton.png");
                    }
                });
        event.registerEntityRenderer(ModEntities.LICH_HUNTER.get(),
                ctx -> new net.minecraft.client.renderer.entity.HumanoidMobRenderer<br.com.murilo.liberthia.magic.boss.LichHunterEntity, net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.magic.boss.LichHunterEntity>>(
                        ctx, new net.minecraft.client.model.HumanoidModel<>(
                                ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER)),
                        0.5F) {
                    @Override
                    public net.minecraft.resources.ResourceLocation getTextureLocation(
                            br.com.murilo.liberthia.magic.boss.LichHunterEntity e) {
                        return new net.minecraft.resources.ResourceLocation(
                                "minecraft", "textures/entity/skeleton/skeleton.png");
                    }
                });

        // r164 CRASH FIX #2: BOOKWYRM (familiar registrado em ModEntities mas sem
        // renderer client). Spawna no spirit_world creature list (gen_r164_horror_spawns.py)
        // — sem renderer = NullPointerException no LevelRenderer.
        event.registerEntityRenderer(ModEntities.BOOKWYRM.get(),
                ctx -> new net.minecraft.client.renderer.entity.EntityRenderer<br.com.murilo.liberthia.observation.entity.BookwyrmEntity>(ctx) {
                    @Override
                    public net.minecraft.resources.ResourceLocation getTextureLocation(
                            br.com.murilo.liberthia.observation.entity.BookwyrmEntity e) {
                        return new net.minecraft.resources.ResourceLocation(
                                "minecraft", "textures/entity/parrot/parrot_blue.png");
                    }
                });
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    private static <T extends br.com.murilo.liberthia.magic.wizard.AbstractWizardEntity> void registerWizardRenderer(
            net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event,
            net.minecraft.world.entity.EntityType<T> type, String texturePath) {
        // r178 FIX: usa a textura REAL passada em texturePath (antes ignorava e
        // retornava evoker.png hardcoded → todos os wizards saíam iguais/bugados).
        final net.minecraft.resources.ResourceLocation tex =
                new net.minecraft.resources.ResourceLocation(texturePath);
        event.registerEntityRenderer(type, ctx -> new net.minecraft.client.renderer.entity.HumanoidMobRenderer<T, net.minecraft.client.model.HumanoidModel<T>>(
                ctx, new net.minecraft.client.model.HumanoidModel<>(
                        ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER)),
                0.5F) {
            @Override
            public net.minecraft.resources.ResourceLocation getTextureLocation(T e) {
                return tex;
            }
        });
    }

    @SubscribeEvent
    public static void onRegisterLayers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(br.com.murilo.liberthia.client.model.BloodWormModel.LAYER,
                br.com.murilo.liberthia.client.model.BloodWormModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        // Pipe Extrator → laranja (0xFF8C00) tinted onde tem tintindex
        event.register((s, l, p, idx) -> 0xFF8C00, ModBlocks.ITEM_EXTRACTOR.get());
        // Pipe Inseridor → verde (0x4CD950) tinted
        event.register((s, l, p, idx) -> 0x4CD950, ModBlocks.ITEM_INSERTER.get());
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((s, idx) -> 0xFF8C00, ModBlocks.ITEM_EXTRACTOR.get().asItem());
        event.register((s, idx) -> 0x4CD950, ModBlocks.ITEM_INSERTER.get().asItem());
    }

    @SubscribeEvent
    public static void onRegisterGuiOverlays(net.minecraftforge.client.event.RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("infection_hud", InfectionHudOverlay.INSTANCE);
        event.registerAboveAll("matter_energy_hud", MatterEnergyHudOverlay.INSTANCE);
        event.registerAboveAll("dna_mutation_hud", DnaMutationOverlay.INSTANCE);
        event.registerAboveAll("radiation_guide_hud", RadiationGuideOverlay.INSTANCE);
        event.registerAboveAll("infection_distortion", InfectionDistortionOverlay.INSTANCE);
        event.registerAboveAll("matter_profile_hud", br.com.murilo.liberthia.client.hud.MatterProfileHud.INSTANCE);
    }

    @SubscribeEvent
    @SuppressWarnings("removal")
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.PURIFICATION_BENCH.get(), PurificationBenchScreen::new);
            // r69: Scribes Table — GUI de crafting de spell parchments
            MenuScreens.register(ModMenuTypes.SCRIBES_TABLE.get(),
                    br.com.murilo.liberthia.client.screen.ScribesTableScreen::new);
            // r73: Imbuement Table screen
            MenuScreens.register(ModMenuTypes.IMBUEMENT_TABLE.get(),
                    br.com.murilo.liberthia.client.screen.ImbuementScreen::new);
            // r77: Spell Binding Pedestal screen
            MenuScreens.register(ModMenuTypes.SPELL_BINDING_PEDESTAL.get(),
                    br.com.murilo.liberthia.client.screen.SpellBindingPedestalScreen::new);
            // r118: Glyph Inscriber — GUI bonita pra craft de reagents → scroll
            MenuScreens.register(ModMenuTypes.GLYPH_INSCRIBER.get(),
                    br.com.murilo.liberthia.client.screen.GlyphInscriberScreen::new);
            // r119: Spell Weaver — GUI pra compor scrolls com modifier glyphs
            MenuScreens.register(ModMenuTypes.SPELL_WEAVER.get(),
                    br.com.murilo.liberthia.client.screen.SpellWeaverScreen::new);
            // r138: Spell Mutator — GUI pra combinar 2 scrolls em hibrido
            MenuScreens.register(ModMenuTypes.SPELL_MUTATOR.get(),
                    br.com.murilo.liberthia.client.screen.SpellMutatorScreen::new);
            // r155 Phase 2: Arcane Workbench — GUI 9 slots (base + 7 mods + output)
            MenuScreens.register(ModMenuTypes.ARCANE_WORKBENCH.get(),
                    br.com.murilo.liberthia.magic.workbench.ArcaneWorkbenchScreen::new);
            // r119: Grimoire — book GUI with 9 scroll slots
            MenuScreens.register(ModMenuTypes.GRIMOIRE.get(),
                    br.com.murilo.liberthia.client.screen.GrimoireScreen::new);
            MenuScreens.register(ModMenuTypes.DARK_MATTER_FORGE.get(), DarkMatterForgeScreen::new);
            MenuScreens.register(ModMenuTypes.MATTER_INFUSER.get(), MatterInfuserScreen::new);
            MenuScreens.register(ModMenuTypes.RESEARCH_TABLE.get(), ResearchTableScreen::new);
            MenuScreens.register(ModMenuTypes.MATTER_TESTER.get(),
                    br.com.murilo.liberthia.client.screen.MatterTesterScreen::new);
            MenuScreens.register(ModMenuTypes.CONTAINMENT_CHAMBER.get(), ContainmentChamberScreen::new);
            MenuScreens.register(ModMenuTypes.MATTER_TRANSMUTER.get(), MatterTransmuterScreen::new);
            MenuScreens.register(ModMenuTypes.DARK_MATTER_ALCHEMIZER.get(), DarkMatterAlchemizerScreen::new);
            MenuScreens.register(ModMenuTypes.DARK_MATTER_GENERATOR.get(),
                    br.com.murilo.liberthia.client.screen.DarkMatterGeneratorScreen::new);
            MenuScreens.register(ModMenuTypes.DARK_MATTER_CHEST.get(),
                    br.com.murilo.liberthia.client.screen.DarkMatterChestScreen::new);
            MenuScreens.register(ModMenuTypes.FRAGMENTED_GENERATOR.get(),
                    br.com.murilo.liberthia.client.screen.FragmentedGeneratorScreen::new);
            MenuScreens.register(ModMenuTypes.CRYSTALLIZER.get(),
                    br.com.murilo.liberthia.client.screen.CrystallizerScreen::new);
            MenuScreens.register(ModMenuTypes.AUTO_FARMER.get(),
                    br.com.murilo.liberthia.client.screen.AutoFarmerScreen::new);
            MenuScreens.register(ModMenuTypes.MATTER_ANALYZER.get(),
                    br.com.murilo.liberthia.client.screen.MatterAnalyzerScreen::new);
            MenuScreens.register(ModMenuTypes.PIPE_FILTER.get(),
                    br.com.murilo.liberthia.client.screen.PipeFilterScreen::new);
            MenuScreens.register(ModMenuTypes.MATTER_PURIFIER.get(),
                    br.com.murilo.liberthia.client.screen.MatterPurifierScreen::new);
            MenuScreens.register(ModMenuTypes.MATTER_PILL_BREWER.get(),
                    br.com.murilo.liberthia.client.screen.MatterPillBrewerScreen::new);
            MenuScreens.register(ModMenuTypes.MATTER_TANK.get(),
                    br.com.murilo.liberthia.client.screen.MatterTankScreen::new);
            MenuScreens.register(ModMenuTypes.MATTER_EXTRACTOR.get(),
                    br.com.murilo.liberthia.client.screen.MatterExtractorScreen::new);
            MenuScreens.register(ModMenuTypes.DIMENSIONAL_EXTRACTOR.get(),
                    br.com.murilo.liberthia.client.screen.DimensionalExtractorScreen::new);
            MenuScreens.register(ModMenuTypes.DARK_MATTER_BATTERY.get(),
                    br.com.murilo.liberthia.client.screen.DarkMatterBatteryScreen::new);
            MenuScreens.register(ModMenuTypes.DIMENSIONAL_CHEST.get(),
                    br.com.murilo.liberthia.client.screen.DimensionalChestScreen::new);
            MenuScreens.register(ModMenuTypes.MATTER_REFINER.get(),
                    br.com.murilo.liberthia.client.screen.MatterRefinerScreen::new);
            MenuScreens.register(ModMenuTypes.WIRELESS_CHARGER.get(),
                    br.com.murilo.liberthia.client.screen.WirelessChargerScreen::new);
            // v0.1.22 r27: Dimensional Antenna
            MenuScreens.register(ModMenuTypes.DIMENSIONAL_ANTENNA.get(),
                    br.com.murilo.liberthia.client.screen.DimensionalAntennaScreen::new);
            // v0.1.22 r28: Quantum Terminal
            MenuScreens.register(ModMenuTypes.QUANTUM_TERMINAL.get(),
                    br.com.murilo.liberthia.client.screen.QuantumTerminalScreen::new);

            // r164: Inscription Table — recipe list à esquerda + 3 slots
            MenuScreens.register(ModMenuTypes.INSCRIPTION_TABLE.get(),
                    br.com.murilo.liberthia.magic.scribe.client.InscriptionTableScreen::new);

            // r165: Scroll Forge — Focus → Scroll GUI bonita
            MenuScreens.register(ModMenuTypes.SCROLL_FORGE.get(),
                    br.com.murilo.liberthia.magic.scribe.client.ScrollForgeScreen::new);

            // r166: Impressora — GUI com lista de pendências + slot de papel
            MenuScreens.register(ModMenuTypes.PRINTER.get(),
                    br.com.murilo.liberthia.client.screen.PrinterScreen::new);

            // r172: Tear de Threads — 5 slots + preview de propriedades
            MenuScreens.register(ModMenuTypes.THREAD_LOOM.get(),
                    br.com.murilo.liberthia.client.screen.ThreadLoomScreen::new);
            MenuScreens.register(ModMenuTypes.ORB_INFUSER.get(),
                    br.com.murilo.liberthia.client.screen.OrbInfuserScreen::new);

            // Sample Vial: model override "filled" baseado no NBT (legacy, mantido).
            net.minecraft.client.renderer.item.ItemProperties.register(
                    br.com.murilo.liberthia.registry.ModItems.SAMPLE_VIAL.get(),
                    new net.minecraft.resources.ResourceLocation(
                            br.com.murilo.liberthia.LiberthiaMod.MODID, "filled"),
                    (stack, level, entity, seed) ->
                            stack.hasTag() && stack.getTag().contains("src") ? 1f : 0f);

            // r155 Phase 3: Factory Spell Scroll — model override por spell ID
            // Cada spell tem um índice fixo (1..42), retornado como float.
            // factory_spell_scroll.json tem overrides matchando cada threshold.
            net.minecraft.client.renderer.item.ItemProperties.register(
                    br.com.murilo.liberthia.registry.ModItems.FACTORY_SPELL_SCROLL.get(),
                    new net.minecraft.resources.ResourceLocation(
                            br.com.murilo.liberthia.LiberthiaMod.MODID, "spell_index"),
                    (stack, level, entity, seed) ->
                            br.com.murilo.liberthia.magic.factory.FactorySpellIndex.indexFor(stack));

            // Sample Vial: matter_type property — escolhe textura por matéria dominante.
            // Valores (sincronizados com sample_vial.json):
            //   0.0 → vazio (sample_vial.png)
            //   0.2 → DM dominante (purple)   threshold 0.1
            //   0.5 → WM dominante (white)    threshold 0.4
            //   0.8 → YM dominante (yellow)   threshold 0.7
            net.minecraft.client.renderer.item.ItemProperties.register(
                    br.com.murilo.liberthia.registry.ModItems.SAMPLE_VIAL.get(),
                    new net.minecraft.resources.ResourceLocation(
                            br.com.murilo.liberthia.LiberthiaMod.MODID, "matter_type"),
                    (stack, level, entity, seed) -> {
                        if (!stack.hasTag()) return 0f;
                        var tag = stack.getTag();
                        if (!tag.contains("src")) return 0f;
                        float dm = tag.getFloat("dm");
                        float wm = tag.getFloat("wm");
                        float ym = tag.getFloat("ym");
                        float max = Math.max(dm, Math.max(wm, ym));
                        if (max <= 0) return 0f;
                        if (max == ym) return 0.8f;
                        if (max == wm) return 0.5f;
                        return 0.2f;  // DM (também é fallback se tudo igual)
                    });

            // r176: EMF Meter — property "emf" acende mais LEDs conforme o nível de
            // horror na região. O CORPO é fixo (geometria idêntica em todos os modelos):
            // só os LEDs do topo mudam de cor/quantidade → NÃO "pula" na mão. O valor já
            // vem suavizado do NBT (EmfMeterItem), então os LEDs sobem/descem liso.
            net.minecraft.client.renderer.item.ItemProperties.register(
                    br.com.murilo.liberthia.registry.ModItems.EMF_METER.get(),
                    new net.minecraft.resources.ResourceLocation(
                            br.com.murilo.liberthia.LiberthiaMod.MODID, "emf"),
                    (stack, lvl, entity, seed) ->
                            br.com.murilo.liberthia.cosmic.emf.EmfMeterItem.readEmf(stack));

            // r178: Núcleo do Abismo → property "crystallized" (0 = abismo, 1 = vazio cristalizado)
            net.minecraft.client.renderer.item.ItemProperties.register(
                    br.com.murilo.liberthia.registry.ModItems.ABYSSAL_CORE.get(),
                    new net.minecraft.resources.ResourceLocation(
                            br.com.murilo.liberthia.LiberthiaMod.MODID, "crystallized"),
                    (stack, lvl, entity, seed) ->
                            br.com.murilo.liberthia.item.AbyssalCoreItem.isCrystallized(stack) ? 1.0F : 0.0F);

            // Cutout render so connection arms transparency works
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ENERGY_CABLE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ITEM_PIPE.get(), RenderType.cutout());

            // Matter pipes — cutout pra suportar transparência das pontas
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MATTER_PIPE_DARK.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MATTER_PIPE_CLEAR.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MATTER_PIPE_YELLOW.get(), RenderType.cutout());
            // Matter tank — translucent pra deixar ver fluido por dentro do vidro
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MATTER_TANK.get(), RenderType.translucent());
            // r173: Potion Jar — translucent pra ver o Source roxo enchendo por dentro
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.POTION_JAR.get(), RenderType.translucent());

            event.enqueueWork(() ->
                    MenuScreens.register(ModMenuTypes.SPIRITUAL_TRADE.get(), SpiritualTradeScreen::new)
            );

            // RenderType registration for transparent/cutout blocks
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SPORE_BLOOM.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.INFECTION_GROWTH.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.WORMHOLE_BLOCK.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CLEAR_MATTER_FLUID_BLOCK.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.YELLOW_MATTER_FLUID_BLOCK.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BLOOD_FLUID_BLOCK.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CHALK_SYMBOL.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.THORN_BRIAR.get(), RenderType.cutout());
            // r143: Spirit World plants (cross shape - needs cutout pra alpha funcionar)
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.WHISPER_PETAL_BUSH.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.GHOST_MUSHROOM.get(), RenderType.cutout());
            // r149: Magic Fire (7 escolas) + Magelight Torch — cutout pra remover fundo preto
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MAGIC_FIRE_FIRE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MAGIC_FIRE_ICE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MAGIC_FIRE_LIGHTNING.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MAGIC_FIRE_BLOOD.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MAGIC_FIRE_ELDRITCH.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MAGIC_FIRE_HOLY.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MAGIC_FIRE_NATURE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MAGELIGHT_TORCH.get(), RenderType.cutout());
            // r34: OCCULT chalks + candles + portal — RENDER LAYER CUTOUT
            // (sem isso, fundo transparente vira PRETO no chão)
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CHALK_MARK_WHITE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CHALK_MARK_GOLDEN.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CHALK_MARK_PURPLE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CHALK_MARK_RED.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CHALK_MARK_BLACK.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CANDLE_WHITE_OCCULT.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CANDLE_GOLDEN_OCCULT.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CANDLE_PURPLE_OCCULT.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CANDLE_RED_OCCULT.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CANDLE_BLACK_OCCULT.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.LOOM_PORTAL.get(), RenderType.translucent());
            // v0.1.20 fix: SANGUINE_SAPLING precisa de render layer cutout pra
            // não renderizar com fundo preto. Bloco cross sem cutout = pixels
            // transparentes da textura viram pretos opacos.
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SANGUINE_SAPLING.get(), RenderType.cutout());
            // Mesma issue: tocha de sangue (torch model usa cutout vanilla).
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BLOOD_TORCH.get(), RenderType.cutout());

            // --- Blood Tree family — sapling (cross model), leaves (cutout_mipped
            //     pra folhas com transparência), door/trapdoor (cutout pra
            //     furos no model vanilla door_bottom_left etc).
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BLOOD_SAPLING.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BLOOD_LEAVES.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BLOOD_DOOR.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BLOOD_TRAPDOOR.get(), RenderType.cutout());
        });
    }

    /** r150: helper pra registrar renderer dos 8 Wooden Horror variants. */
    private static void registerWoodenHorrorRenderer(
            net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event,
            net.minecraft.world.entity.EntityType<br.com.murilo.liberthia.cosmic.horror.entity.WoodenHorrorEntity> type,
            String textureName) {
        event.registerEntityRenderer(type,
                ctx -> new net.minecraft.client.renderer.entity.HumanoidMobRenderer<br.com.murilo.liberthia.cosmic.horror.entity.WoodenHorrorEntity, net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.cosmic.horror.entity.WoodenHorrorEntity>>(
                        ctx,
                        new net.minecraft.client.model.HumanoidModel<>(
                                ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER)),
                        0.5F) {
                    @Override
                    public net.minecraft.resources.ResourceLocation getTextureLocation(
                            br.com.murilo.liberthia.cosmic.horror.entity.WoodenHorrorEntity e) {
                        return new net.minecraft.resources.ResourceLocation("liberthia",
                                "textures/entity/" + textureName + ".png");
                    }
                });
    }
}
