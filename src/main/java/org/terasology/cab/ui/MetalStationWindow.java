/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.cab.ui;

import org.terasology.cab.CopperAndBronze;
import org.terasology.crafting.ui.workstation.StationAvailableRecipesWidget;
import org.terasology.engine.Time;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.heat.HeatUtils;
import org.terasology.heat.component.HeatProducerComponent;
import org.terasology.heat.ui.TermometerWidget;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.registry.CoreRegistry;
import org.terasology.rendering.nui.CoreScreenLayer;
import org.terasology.rendering.nui.databinding.Binding;
import org.terasology.rendering.nui.layers.ingame.inventory.InventoryGrid;
import org.terasology.rendering.nui.widgets.UIButton;
import org.terasology.rendering.nui.widgets.UILoadBar;
import org.terasology.was.ui.VerticalTextureProgressWidget;
import org.terasology.workstation.component.WorkstationInventoryComponent;
import org.terasology.workstation.component.WorkstationProcessingComponent;
import org.terasology.workstation.ui.WorkstationUI;
import org.terasology.world.BlockEntityRegistry;

import java.util.List;

/**
 * @author Marcin Sciesinski <marcins78@gmail.com>
 */
public class MetalStationWindow extends CoreScreenLayer implements WorkstationUI {

    private InventoryGrid ingredientsInventory;
    private InventoryGrid toolsInventory;
    private TermometerWidget temperature;
    private VerticalTextureProgressWidget burn;
    private InventoryGrid fuelInput;
    private StationAvailableRecipesWidget availableRecipes;
    private InventoryGrid resultInventory;
    private UILoadBar craftingProgress;
    private InventoryGrid upgrades;
    private UIButton upgradeButton;

    @Override
    public void initialise() {
        ingredientsInventory = find("ingredientsInventory", InventoryGrid.class);
        upgrades = find("upgradesInventory", InventoryGrid.class);
        upgradeButton = find("upgradeButton", UIButton.class);

        upgradeButton.setText("Upgrade");

        toolsInventory = find("toolsInventory", InventoryGrid.class);

        temperature = find("temperature", TermometerWidget.class);

        burn = find("burn", VerticalTextureProgressWidget.class);
        burn.setMinY(76);
        burn.setMaxY(4);

        fuelInput = find("fuelInput", InventoryGrid.class);

        availableRecipes = find("availableRecipes", StationAvailableRecipesWidget.class);

        craftingProgress = find("craftingProgress", UILoadBar.class);

        resultInventory = find("resultInventory", InventoryGrid.class);

        InventoryGrid playerInventory = find("playerInventory", InventoryGrid.class);

        playerInventory.setTargetEntity(CoreRegistry.get(LocalPlayer.class).getCharacterEntity());
        playerInventory.setCellOffset(10);
        playerInventory.setMaxCellCount(30);
    }

    @Override
    public void initializeWorkstation(final EntityRef station) {
        WorkstationInventoryComponent workstationInventory = station.getComponent(WorkstationInventoryComponent.class);
        WorkstationInventoryComponent.SlotAssignment inputAssignments = workstationInventory.slotAssignments.get("INPUT");
        WorkstationInventoryComponent.SlotAssignment toolAssignments = workstationInventory.slotAssignments.get("TOOL");
        WorkstationInventoryComponent.SlotAssignment resultAssignments = workstationInventory.slotAssignments.get("OUTPUT");
        WorkstationInventoryComponent.SlotAssignment upgradeAssignments = workstationInventory.slotAssignments.get("UPGRADE");
        WorkstationInventoryComponent.SlotAssignment fuelAssignments = workstationInventory.slotAssignments.get("FUEL");

        ingredientsInventory.setTargetEntity(station);
        ingredientsInventory.setCellOffset(inputAssignments.slotStart);
        ingredientsInventory.setMaxCellCount(inputAssignments.slotCount);

        upgrades.setTargetEntity(station);
        upgrades.setCellOffset(upgradeAssignments.slotStart);
        upgrades.setMaxCellCount(upgradeAssignments.slotCount);

        toolsInventory.setTargetEntity(station);
        toolsInventory.setCellOffset(toolAssignments.slotStart);
        toolsInventory.setMaxCellCount(toolAssignments.slotCount);

        setupTemperatureWidget(station);

        burn.bindValue(
                new Binding<Float>() {
                    @Override
                    public Float get() {
                        HeatProducerComponent heatProducer = station.getComponent(HeatProducerComponent.class);
                        List<HeatProducerComponent.FuelSourceConsume> consumedFuel = heatProducer.fuelConsumed;
                        if (consumedFuel.size() == 0) {
                            return 0f;
                        }
                        long gameTime = CoreRegistry.get(Time.class).getGameTimeInMs();

                        HeatProducerComponent.FuelSourceConsume lastConsumed = consumedFuel.get(consumedFuel.size() - 1);
                        if (gameTime > lastConsumed.startTime + lastConsumed.burnLength) {
                            return 0f;
                        }
                        return 1f - (1f * (gameTime - lastConsumed.startTime) / lastConsumed.burnLength);
                    }

                    @Override
                    public void set(Float value) {
                    }
                });

        fuelInput.setTargetEntity(station);
        fuelInput.setCellOffset(fuelAssignments.slotStart);
        fuelInput.setMaxCellCount(fuelAssignments.slotCount);

        availableRecipes.setStation(station);

        craftingProgress.bindVisible(
                new Binding<Boolean>() {
                    @Override
                    public Boolean get() {
                        WorkstationProcessingComponent processing = station.getComponent(WorkstationProcessingComponent.class);
                        if (processing == null) {
                            return false;
                        }
                        WorkstationProcessingComponent.ProcessDef heatingProcess = processing.processes.get(CopperAndBronze.BASIC_METALCRAFTING_PROCESS_TYPE);
                        return heatingProcess != null;
                    }

                    @Override
                    public void set(Boolean value) {
                    }
                }
        );
        craftingProgress.bindValue(
                new Binding<Float>() {
                    @Override
                    public Float get() {
                        WorkstationProcessingComponent processing = station.getComponent(WorkstationProcessingComponent.class);
                        if (processing == null) {
                            return 1f;
                        }
                        WorkstationProcessingComponent.ProcessDef heatingProcess = processing.processes.get(CopperAndBronze.BASIC_METALCRAFTING_PROCESS_TYPE);
                        if (heatingProcess == null) {
                            return 1f;
                        }

                        long gameTime = CoreRegistry.get(Time.class).getGameTimeInMs();

                        return 1f * (gameTime - heatingProcess.processingStartTime) / (heatingProcess.processingFinishTime - heatingProcess.processingStartTime);
                    }

                    @Override
                    public void set(Float value) {
                    }
                }
        );

        resultInventory.setTargetEntity(station);
        resultInventory.setCellOffset(resultAssignments.slotStart);
        resultInventory.setMaxCellCount(resultAssignments.slotCount);
    }

    private void setupTemperatureWidget(final EntityRef station) {
        temperature.bindMaxTemperature(
                new Binding<Float>() {
                    @Override
                    public Float get() {
                        HeatProducerComponent producer = station.getComponent(HeatProducerComponent.class);
                        return producer.maximumTemperature;
                    }

                    @Override
                    public void set(Float value) {
                    }
                }
        );
        temperature.setMinTemperature(20f);

        temperature.bindTemperature(
                new Binding<Float>() {
                    @Override
                    public Float get() {
                        return HeatUtils.calculateHeatForEntity(station, CoreRegistry.get(BlockEntityRegistry.class));
                    }

                    @Override
                    public void set(Float value) {
                    }
                });
        temperature.bindTooltip(
                new Binding<String>() {
                    @Override
                    public String get() {
                        return Math.round(HeatUtils.calculateHeatForEntity(station, CoreRegistry.get(BlockEntityRegistry.class))) + " C";
                    }

                    @Override
                    public void set(String value) {
                    }
                }
        );
    }

    @Override
    public boolean isModal() {
        return false;
    }
}
