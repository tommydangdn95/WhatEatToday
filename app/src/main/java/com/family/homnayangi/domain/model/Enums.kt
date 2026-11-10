package com.family.homnayangi.domain.model

/** XAO = xào có thịt/hải sản; RAU = rau luộc, rau xào chay, nộm. */
enum class DishCategory(val label: String) {
    MAN("Mặn"), XAO("Xào"), RAU("Rau"), CANH("Canh")
}

enum class Season(val label: String) {
    NONG("Nóng"), LANH("Lạnh"), QUANH_NAM("Quanh năm")
}

/** CHUNG = món cả nước đều nấu. */
enum class Region { BAC, CHUNG, TRUNG, NAM }

enum class MealType(val label: String) {
    TRUA("Trưa"), TOI("Tối")
}

enum class IngredientGroup(val label: String) {
    THIT("Thịt"), CA_HAI_SAN("Cá & hải sản"), TRUNG_DAU("Trứng & đậu"), RAU_CU("Rau củ"), KHAC("Khác");

    /** Nhóm dùng để kiểm tra "không trùng nguyên liệu chính" trong một mâm (F02). */
    val isProtein: Boolean get() = this == THIT || this == CA_HAI_SAN
}

enum class WeatherClass { NONG, MAT_LANH, BINH_THUONG }
