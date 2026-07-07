#
# Copyright (C) 2025 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit_only.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Inherit some common Halcyon stuff.
$(call inherit-product, vendor/halcyon/config/common.mk)

# Inherit from klee device
$(call inherit-product, device/xiaomi/klee/device.mk)

# Device build flags
TARGET_SHIPS_MIUICAMERA := true

# Halcyon build flags
TARGET_SUPPORTS_GOOGLE_RECORDER := true

# Device manufacturer
PRODUCT_DEVICE := klee
PRODUCT_NAME := halcyon_klee
PRODUCT_BRAND := POCO
PRODUCT_MODEL := 2511FPC34G
PRODUCT_MANUFACTURER := xiaomi

PRODUCT_SYSTEM_NAME := klee_global
PRODUCT_SYSTEM_DEVICE := klee
PRODUCT_GMS_CLIENTID_BASE := android-xiaomi

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildDesc="missi-user 16 BP2A.250605.031.A3 OS3.0.304.0.WPJMIXM release-keys" \
    BuildFingerprint=POCO/klee_global/klee:15/AP3A.240905.015.A2/OS3.0.304.0.WPJMIXM:user/release-keys \
    DeviceName=$(PRODUCT_SYSTEM_DEVICE) \
    DeviceProduct=$(PRODUCT_SYSTEM_NAME)
