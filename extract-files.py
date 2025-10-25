#!/usr/bin/env -S PYTHONPATH=../../../tools/extract-utils python3
#
# SPDX-FileCopyrightText: 2024 The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

from extract_utils.fixups_blob import (
    blob_fixup,
    blob_fixups_user_type,
)

from extract_utils.main import (
    ExtractUtils,
    ExtractUtilsModule,
)

from extract_utils.fixups_lib import (
    lib_fixup_remove_arch_suffix,
    lib_fixups_user_type,
    libs_clang_rt_ubsan,
)

namespace_imports = [
    'device/xiaomi/rodin',
    'hardware/mediatek',
    'hardware/mediatek/libmtkperf_client',
    'hardware/lineage/compat',
    'hardware/xiaomi',
]

lib_fixups: lib_fixups_user_type = {
    libs_clang_rt_ubsan: lib_fixup_remove_arch_suffix,
}

def lib_fixup_vendor_suffix(lib: str, partition: str, *args, **kwargs):
    return f'{lib}-{partition}' if partition == 'vendor' else None


lib_fixups: lib_fixups_user_type = {
    **lib_fixups,
    ('vendor.xiaomi.hw.touchfeature-V1-ndk'): lib_fixup_vendor_suffix,
    ('vendor.xiaomi.hardware.fingerprintextension-V1-ndk'): lib_fixup_vendor_suffix,
}

blob_fixups: blob_fixups_user_type = {
    ('odm/lib64/libmt_mitee.so', 'vendor/bin/hw/android.hardware.security.keymint@3.0-service.mitee'): blob_fixup()
        .replace_needed('android.hardware.security.keymint-V3-ndk.so', 'android.hardware.security.keymint-V3-ndk-v34.so'),
    'odm/bin/hw/vendor.xiaomi.sensor.citsensorservice.aidl': blob_fixup()
        .add_needed('libui_shim.so'),
    (
        'vendor/bin/mnld',
        'vendor/lib64/mt6899/libpqconfig.so',
        'vendor/lib64/mt6899/libaalservice.so',
        'odm/bin/hw/vendor.xiaomi.hw.touchfeature-service',
        'odm/bin/hw/vendor.xiaomi.sensor.citsensorservice.aidl',
        'odm/lib64/hw/displayfeature.default.so',
        'odm/lib64/libpaperMode.so',
        'odm/lib64/libmiBrightness.so',
        'odm/lib64/libmiSensorCtrl.so',
        'odm/lib64/libcolortempmode.so',
        'odm/lib64/libtruetone.so',
        'odm/lib64/libsre.so',
        'odm/lib64/libsdr2hdr.so',
        'odm/lib64/libdither.so',
        'odm/lib64/libhistprocess.so',
        'odm/lib64/libadaptivehdr.so',
        'odm/lib64/librhytheyecare.so',
        'odm/lib64/libflatmode.so',
        'odm/lib64/libvideomode.so',
    ): blob_fixup()
        .replace_needed('android.hardware.sensors-V2-ndk.so', 'android.hardware.sensors-V3-ndk.so'),
    (
        'vendor/bin/hw/mt6899/android.hardware.graphics.allocator-V2-service-mediatek.mt6899',
        'vendor/lib64/libcodec2_fsr.so',
        'vendor/lib64/libaimemc.so',
        'vendor/lib64/libcodec2_fsr.so',
        'vendor/lib64/libcodec2_vpp_AIMEMC_plugin.so',
        'vendor/lib64/libcodec2_vpp_AISR_plugin.so',
        'vendor/lib64/libgpud.so',
        'vendor/lib64/mt6899/libmtkcam_grallocutils.so',
        'vendor/lib64/libmtkcam_grallocutils_aidlv2helper.so',
        'vendor/lib64/egl/mt6899/libGLES_mali.so',
        'vendor/lib64/hw/mt6899/android.hardware.graphics.allocator-V2-mediatek.so',
        'vendor/lib64/hw/mt6899/mapper.mediatek.so',
        'vendor/lib64/vendor.mediatek.hardware.camera.isphal-V1-ndk.so',
        'vendor/lib64/vendor.mediatek.hardware.pq_aidl-V2-ndk.so',
        'vendor/lib64/vendor.mediatek.hardware.pq_aidl-V4-ndk.so',
        'vendor/lib64/vendor.mediatek.hardware.pq_aidl-V7-ndk.so',
    ): blob_fixup()
        .replace_needed('android.hardware.graphics.common-V5-ndk.so', 'android.hardware.graphics.common-V6-ndk.so'),
    (
        'vendor/lib64/vendor.xiaomi.hardware.camera.injection-client.so',
        'vendor/lib64/vendor.xiaomi.hardware.camera.injection-V1-ndk.so'
    ): blob_fixup()
        .replace_needed('android.hardware.camera.device-V1-ndk.so', 'android.hardware.camera.device-V2-ndk.so'),
    (
        'vendor/lib64/libcamera2ndk_vendor.so'
    ): blob_fixup()
        .replace_needed('android.frameworks.cameraservice.service-V2-ndk.so', 'android.frameworks.cameraservice.service-V3-ndk.so')
        .replace_needed('android.frameworks.cameraservice.device-V2-ndk', 'android.frameworks.cameraservice.device-V3-ndk'),
    (
        'vendor/lib64/vendor.xiaomi.hardware.camera.injection-service.so'
    ): blob_fixup()
        .replace_needed('android.hardware.camera.device-V1-ndk.so', 'android.hardware.camera.device-V2-ndk.so'),
    ('vendor/lib64/libmicamera_hal_core.so',
     'vendor/lib64/libcameraopt.so',
     'vendor/lib64/mt6899/libcam.hal3a.so',
     'vendor/lib64/mt6899/libcam.hal3a.ctrl.so',
     'vendor/lib64/libmialgoengine.so',
     'vendor/lib64/mt6899/libmtkcam_taskmgr.so',
     'vendor/lib64/hw/hwcomposer.mtk_common.so'): blob_fixup()
        .add_needed('libprocessgroup_shim.so'),
    ('vendor/lib64/mt6899/libneuralnetworks_sl_driver_mtk_prebuilt.so',
     'odm/lib64/libwa_widelens_undistort.so',
     'odm/lib64/libarcsoft_beautyshot.so',
     'vendor/lib64/libMiPhotoFilter.so',
     'odm/lib64/libMiEmojiEffect.so',
     'vendor/lib64/mt6899/libneuron_adapter_mgvi.so',
     'odm/lib64/libMiVideoFilter.so'): blob_fixup()
        .clear_symbol_version('AHardwareBuffer_allocate')
        .clear_symbol_version('AHardwareBuffer_createFromHandle')
        .clear_symbol_version('AHardwareBuffer_describe')
        .clear_symbol_version('AHardwareBuffer_getNativeHandle')
        .clear_symbol_version('AHardwareBuffer_isSupported')
        .clear_symbol_version('AHardwareBuffer_lock')
        .clear_symbol_version('AHardwareBuffer_lockPlanes')
        .clear_symbol_version('AHardwareBuffer_release')
        .clear_symbol_version('AHardwareBuffer_unlock'),
    'vendor/lib64/libultrahdr_rodin.so': blob_fixup()
        .replace_needed('libjpegencoder.so', 'libjpegencoder_rodin.so')
        .replace_needed('libjpegdecoder.so', 'libjpegdecoder_rodin.so'),
    ('odm/lib64/camera/plugins/capture/com.xiaomi.plugin.gainmap.so',
     'odm/lib64/camera/plugins/capture/com.xiaomi.plugin.jpegrAggr.so'): blob_fixup()
        .replace_needed('libultrahdr.so', 'libultrahdr_rodin.so'),
    'vendor/etc/vintf/manifest/manifest_media_c2_default.xml': blob_fixup()
        .regex_replace(r'\s*<fqname>IComponentStore/dolby</fqname>\s*', ''),
    'vendor/lib64/libmisoundfx_mtk_aidl_ext.so': blob_fixup()
        .replace_needed('android.media.audio.common.types-V3-ndk.so', 'android.media.audio.common.types-V4-ndk.so')
        .replace_needed('android.hardware.audio.effect-V2-ndk.so', 'android.hardware.audio.effect-V3-ndk.so'),
    'vendor/lib64/hw/audio.primary.mediatek.so': blob_fixup()
        .replace_needed('libalsautils.so', 'libalsautils-v34.so'),
    'vendor/lib64/libdlbdsservice.so': blob_fixup()
        .replace_needed("libstagefright_foundation.so", "libstagefright_foundation-v33.so"),
    'vendor/bin/hw/mtkfusionrild': blob_fixup()
        .add_needed('libutils-v32.so'),
    "vendor/etc/init/android.hardware.wifi-service-lazy-mediatek.rc": blob_fixup()
        .regex_replace("lazy", "lazy-mediatek"),
}  # fmt: skip

module = ExtractUtilsModule(
    'rodin',
    'xiaomi',
    blob_fixups=blob_fixups,
    lib_fixups=lib_fixups,
    namespace_imports=namespace_imports,
    add_firmware_proprietary_file=True,
)

if __name__ == '__main__':
    utils = ExtractUtils.device(module)
    utils.run()
