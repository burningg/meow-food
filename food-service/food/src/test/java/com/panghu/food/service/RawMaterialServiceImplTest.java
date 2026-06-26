package com.panghu.food.service;

import com.panghu.food.dto.RawMaterialMatchRow;
import com.panghu.food.dto.RawMaterialResponse;
import com.panghu.food.entity.RawMaterial;
import com.panghu.food.mapper.RawMaterialMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RawMaterialServiceImplTest {
    private final RawMaterialMapper rawMaterialMapper = mock(RawMaterialMapper.class);
    private final DishAiService dishAiService = mock(DishAiService.class);
    private final RawMaterialServiceImpl rawMaterialService = new RawMaterialServiceImpl(rawMaterialMapper, dishAiService);

    @Test
    void ensureRawMaterialsDeduplicatesQueriesMissingAndInsertsAiResult() {
        when(rawMaterialMapper.selectMatchedNames(List.of("土豆", "牛肉"))).thenReturn(List.of("土豆"));
        when(dishAiService.completeRawMaterials(List.of("牛肉"))).thenReturn(List.of(material("牛肉")));

        rawMaterialService.ensureRawMaterialsAsync(Arrays.asList(" 土豆 ", "土豆", "牛肉", "", null));

        verify(rawMaterialMapper).selectMatchedNames(List.of("土豆", "牛肉"));
        verify(dishAiService).completeRawMaterials(List.of("牛肉"));
        ArgumentCaptor<List<RawMaterial>> captor = ArgumentCaptor.forClass(List.class);
        verify(rawMaterialMapper).insertIgnoreBatch(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getName()).isEqualTo("牛肉");
        assertThat(captor.getValue().get(0).getId()).isNotBlank();
        assertThat(captor.getValue().get(0).getCreatedAt()).isNotNull();
        assertThat(captor.getValue().get(0).getUpdatedAt()).isNotNull();
    }

    @Test
    void ensureRawMaterialsSkipsAiWhenAllNamesExist() {
        when(rawMaterialMapper.selectMatchedNames(List.of("土豆"))).thenReturn(List.of("土豆"));

        rawMaterialService.ensureRawMaterialsAsync(List.of("土豆"));

        verify(rawMaterialMapper).selectMatchedNames(List.of("土豆"));
        verifyNoInteractions(dishAiService);
        verify(rawMaterialMapper, never()).insertIgnoreBatch(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ensureRawMaterialsTreatsCommonNameMatchAsExisting() {
        when(rawMaterialMapper.selectMatchedNames(List.of("蒜", "牛肉"))).thenReturn(List.of("蒜"));
        when(dishAiService.completeRawMaterials(List.of("牛肉"))).thenReturn(List.of(material("牛肉")));

        rawMaterialService.ensureRawMaterialsAsync(List.of("蒜", "牛肉"));

        verify(rawMaterialMapper).selectMatchedNames(List.of("蒜", "牛肉"));
        verify(dishAiService).completeRawMaterials(List.of("牛肉"));
    }

    @Test
    void ensureRawMaterialsDoesNotThrowWhenAiFails() {
        when(rawMaterialMapper.selectMatchedNames(List.of("牛肉"))).thenReturn(List.of());
        when(dishAiService.completeRawMaterials(List.of("牛肉"))).thenThrow(new RuntimeException("ai down"));

        assertThatCode(() -> rawMaterialService.ensureRawMaterialsAsync(List.of("牛肉")))
                .doesNotThrowAnyException();

        verify(rawMaterialMapper, never()).insertIgnoreBatch(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void findMatchedRawMaterialsDeduplicatesAndKeepsInputOrder() {
        when(rawMaterialMapper.selectMatchedMaterials(List.of("蒜", "牛肉"))).thenReturn(List.of(
                matchRow("牛肉", "牛肉"),
                matchRow("蒜", "大蒜")
        ));

        List<RawMaterialResponse> result = rawMaterialService.findMatchedRawMaterials(
                Arrays.asList(" 蒜 ", "牛肉", "蒜", "", null));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getIngredientName()).isEqualTo("蒜");
        assertThat(result.get(0).getName()).isEqualTo("大蒜");
        assertThat(result.get(0).getCommonNames()).contains("蒜别名");
        assertThat(result.get(0).getCalorieEstimate()).isEqualTo("约250kcal/100g");
        assertThat(result.get(1).getIngredientName()).isEqualTo("牛肉");
        verify(rawMaterialMapper).selectMatchedMaterials(List.of("蒜", "牛肉"));
        verifyNoInteractions(dishAiService);
    }

    @Test
    void findMatchedRawMaterialsSkipsQueryWhenInputIsEmpty() {
        List<RawMaterialResponse> result = rawMaterialService.findMatchedRawMaterials(Arrays.asList(" ", null));

        assertThat(result).isEmpty();
        verifyNoInteractions(rawMaterialMapper, dishAiService);
    }

    private RawMaterial material(String name) {
        RawMaterial material = new RawMaterial();
        material.setName(name);
        material.setCommonNames(name + "," + name + "别名," + name + "学名");
        material.setSteamTime("约20分钟");
        material.setBoilTime("约40分钟");
        material.setFryTime("约5分钟");
        material.setBakeTime("约25分钟");
        material.setStirFryTime("约4分钟");
        material.setDefaultHeatTemperature("中火");
        material.setAllergenFlag("无常见过敏原");
        material.setCalorieEstimate("约250kcal/100g");
        material.setNutritionInfo("富含蛋白质");
        material.setSubstituteIngredients("可用羊肉");
        material.setCategory("肉");
        return material;
    }

    private RawMaterialMatchRow matchRow(String queryName, String name) {
        RawMaterialMatchRow row = new RawMaterialMatchRow();
        row.setQueryName(queryName);
        row.setName(name);
        row.setCommonNames(name + "," + queryName + "别名," + name + "学名");
        row.setSteamTime("约20分钟");
        row.setBoilTime("约40分钟");
        row.setFryTime("约5分钟");
        row.setBakeTime("约25分钟");
        row.setStirFryTime("约4分钟");
        row.setDefaultHeatTemperature("中火");
        row.setAllergenFlag("无常见过敏原");
        row.setCalorieEstimate("约250kcal/100g");
        row.setNutritionInfo("富含蛋白质");
        row.setSubstituteIngredients("可用羊肉");
        row.setCategory("肉");
        return row;
    }
}
