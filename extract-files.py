#!/usr/bin/env -S PYTHONPATH=../../../tools/extract-utils python3
#
# SPDX-FileCopyrightText: 2024 The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

from extract_utils.fixups_blob import (
    BlobFixupCtx,
    File,
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

from extract_utils.tools import (
    llvm_objdump_path,
)

from extract_utils.utils import (
    run_cmd,
)

namespace_imports = [
    'device/xiaomi/klee',
    'hardware/mediatek',
    'hardware/mediatek/libaedv',
    'hardware/mediatek/libmtkperf_client',
    'hardware/xiaomi',
]

def blob_fixup_graphic_buffer_size(
    ctx: BlobFixupCtx,
    file: File,
    file_path: str,
    *args,
    **kwargs,
):
    for line in run_cmd(
        [
            llvm_objdump_path,
            '--disassemble-all',
            file_path,
        ]
    ).splitlines():
        line = line.split(maxsplit=5)
        if len(line) != 6:
            continue
        # The size of GraphicBuffer changed from 0x100 to 0xd30
        offset, _, instruction, register, value, _ = line
        if instruction == 'mov' and register[:-1] == 'w0' and value == '#0x100':
            with open(file_path, 'rb+') as f:
                f.seek(int(offset[:-1], 16))
                f.write(b'\x00\xa6\x81\x52')  # AArch64 mov w0, #0xd30

lib_fixups: lib_fixups_user_type = {
    libs_clang_rt_ubsan: lib_fixup_remove_arch_suffix,
}

def lib_fixup_vendor_suffix(lib: str, partition: str, *args, **kwargs):
    return f'{lib}-{partition}' if partition == 'vendor' else None

lib_fixups: lib_fixups_user_type = {
    **lib_fixups,
    ('vendor.xiaomi.hw.touchfeature-V1-ndk'): lib_fixup_vendor_suffix,
    ('vendor.xiaomi.hardware.fingerprintextension-V1-ndk'): lib_fixup_vendor_suffix,
    (
        'libneuron_graph_delegate.mtk',
        'libtflite_mtk',
        'vendor.mediatek.hardware.apuware.apusys@2.0',
        'vendor.mediatek.hardware.apuware.apusys@2.1',
        'vendor.mediatek.hardware.apuware.hmp@1.0',
        'vendor.mediatek.hardware.apuware.utils@2.0',
        'vendor.mediatek.hardware.videotelephony@1.0',
        'libXMFaceFocus',
    ): lib_fixup_vendor_suffix,
}

blob_fixups: blob_fixups_user_type = {
    ('odm/lib64/libmt_mitee.so', 'vendor/bin/hw/android.hardware.security.keymint@3.0-service.mitee', 'odm/lib64/libgoogleid.so'): blob_fixup()
        .replace_needed('android.hardware.security.keymint-V3-ndk.so', 'android.hardware.security.keymint-V3-ndk-v36.so'),
    (
        'vendor/bin/mnld',
        'vendor/lib64/mt6899/libpqconfig.so',
        'vendor/lib64/mt6899/libaalservice.so',
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
        'odm/lib64/libdynamicelvss.so',
    ): blob_fixup()
        .replace_needed('android.hardware.sensors-V2-ndk.so', 'android.hardware.sensors-V3-ndk.so'),
    ('odm/bin/hw/vendor.xiaomi.sensor.citsensorservice.aidl', 'odm/lib64/hw/displayfeature.default.so'): blob_fixup()
        .replace_needed('android.hardware.sensors-V2-ndk.so', 'android.hardware.sensors-V3-ndk.so')
        .replace_needed('libtinyxml2.so', 'libtinyxml2-v34.so'),
    (
        'vendor/bin/hw/mt6899/android.hardware.graphics.allocator-V2-service-mediatek.mt6899',
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
        .replace_needed('android.hardware.graphics.common-V5-ndk.so', 'android.hardware.graphics.common-V7-ndk.so'),
    'vendor/lib64/libcodec2_fsr.so': blob_fixup()
        .call(blob_fixup_graphic_buffer_size)
        .replace_needed('android.hardware.graphics.common-V5-ndk.so', 'android.hardware.graphics.common-V7-ndk.so'),
    'vendor/lib64/libcodec2_aidl_prebuilt.so': blob_fixup()
        .replace_needed('libcodec2_hal_common.so', 'libcodec2_hal_common_prebuilt.so')
        .replace_needed('libcodec2_vndk.so', 'libcodec2_vndk_prebuilt.so')
        .replace_needed('libcodec2_hidl_plugin.so', 'libcodec2_hidl_plugin_prebuilt.so'),
    (
        'vendor/bin/hw/android.hardware.media.c2-mediatek-64b',
        'vendor/bin/hw/vendor.dolby.media.c2-default-service-dax',
        'vendor/bin/hw/vendor.dolby.media.c2-service-vision',
        'vendor/lib64/c2.dolby.client.so',
        'vendor/lib64/c2.dolby.hevc.dec.so',
        'vendor/lib64/c2.dolby.hevc.sec.dec.so',
        'vendor/lib64/libcodec2_mtk_vdec.so',
        'vendor/lib64/libcodec2_mtk_venc.so'
    ): blob_fixup()
        .replace_needed('libcodec2_aidl.so', 'libcodec2_aidl_prebuilt.so'),
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
    ('vendor/lib64/libmialgoengine.so',
     'vendor/lib64/libcom.xiaomi.grallocutils.so',
     'vendor/lib64/libmicamera_adapter.so',
     'vendor/lib64/mt6899/libmtkcam_hal_core_device.so',
     'odm/lib64/camera/dynamicplugins/com.xiaomi.plugin.filter.so'): blob_fixup()
        .call(blob_fixup_graphic_buffer_size),
    'vendor/lib64/libmicamera_hal_core.so': blob_fixup()
        .replace_needed('libtinyxml2.so', 'libtinyxml2-v34.so')
        .call(blob_fixup_graphic_buffer_size),
    ('vendor/lib64/mt6899/libneuralnetworks_sl_driver_mtk_prebuilt.so',
     'vendor/lib64/libmcve.so',
     'odm/lib64/libwa_widelens_undistort.so',
     'odm/lib64/libarcsoft_beautyshot.so',
     'vendor/lib64/libMiPhotoFilter.so',
     'odm/lib64/libMiEmojiEffect.so',
     'vendor/lib64/mt6899/libneuron_adapter_mgvi.so',
     'odm/lib64/libMiVideoFilter.so',
     'odm/lib64/libAncHumanPreviewBokeh.so',
     ): blob_fixup()
        .clear_symbol_version('AHardwareBuffer_allocate')
        .clear_symbol_version('AHardwareBuffer_createFromHandle')
        .clear_symbol_version('AHardwareBuffer_describe')
        .clear_symbol_version('AHardwareBuffer_getNativeHandle')
        .clear_symbol_version('AHardwareBuffer_isSupported')
        .clear_symbol_version('AHardwareBuffer_lock')
        .clear_symbol_version('AHardwareBuffer_lockPlanes')
        .clear_symbol_version('AHardwareBuffer_release')
        .clear_symbol_version('AHardwareBuffer_unlock'),
    'vendor/lib64/libultrahdr_klee.so': blob_fixup()
        .replace_needed('libjpegencoder.so', 'libjpegencoder_klee.so')
        .replace_needed('libjpegdecoder.so', 'libjpegdecoder_klee.so'),
    ('odm/lib64/camera/dynamicplugins/com.xiaomi.plugin.gainmap.so',
     'odm/lib64/camera/dynamicplugins/com.xiaomi.plugin.jpegrAggr.so'): blob_fixup()
        .replace_needed('libultrahdr.so', 'libultrahdr_klee.so'),
    'vendor/lib64/libdlbdsservice.so': blob_fixup()
        .replace_needed("libstagefright_foundation.so", "libstagefright_foundation-v33.so"),
    'vendor/bin/hw/mtkfusionrild': blob_fixup()
        .add_needed('libutils-v33.so'),
    "odm/bin/hw/vendor.xiaomi.hw.touchfeature-service": blob_fixup()
        .replace_needed('android.hardware.sensors-V2-ndk.so', 'android.hardware.sensors-V3-ndk.so')
        .replace_needed('vendor.xiaomi.hw.touchfeature-V1-ndk.so', 'vendor.xiaomi.hw.touchfeature-V1-ndk-prebuilt.so'),
    (
        'odm/lib64/libmiXmlParser.so',
        'vendor/lib64/hw/mt6899/vendor.mediatek.hardware.pq_aidl-impl.so',
        'vendor/lib64/hw/audio.primary.mediatek.so',
        'vendor/lib64/mt6899/lib3a.custom.ae.flow.so',
        'vendor/lib64/mt6899/libmmlpqImpl.so',
        'vendor/lib64/libaudiocloudctrl.so',
        'vendor/lib64/libpqxmlflagparser.so',
        'vendor/lib64/libpqxmlparser.so',
        'vendor/lib64/librt_extamp_intf.so',
        'vendor/lib64/libsilkybrightnesscore.so',
        'vendor/lib64/libmicamera_aidl_provider.so',
        'vendor/lib64/libxlog.so',
        'vendor/lib64/libHardwareBacklightcore.so'
    ): blob_fixup()
        .replace_needed('libtinyxml2.so', 'libtinyxml2-v34.so'),
    (
        'vendor/lib64/mt6899/libmtkcam_taskmgr.so',
        'vendor/lib64/libcameraopt.so'
    ): blob_fixup()
        .add_needed('libprocessgroup_shim.so'),
    'vendor/lib64/hw/hwcomposer.mtk_common.so': blob_fixup()
        .add_needed('libprocessgroup_shim.so')
        .replace_needed('libtinyxml2.so', 'libtinyxml2-v34.so'),
    'vendor/bin/hw/android.hardware.audio.service-aidl.mediatek': blob_fixup()
        .replace_needed('android.media.audio.common.types-V5-ndk.so', 'android.media.audio.common.types-V3-ndk.so')
        .replace_needed('libaudio_aidl_conversion_common_ndk.so', 'libaudio_aidl_conversion_common_ndk_prebuilt.so'),
    'vendor/lib64/hw/android.hardware.audio.effect.aidl-impl-mediatek.so': blob_fixup()
        .replace_needed('android.media.audio.common.types-V5-ndk.so', 'android.media.audio.common.types-V3-ndk.so')
        .replace_needed('libtinyxml2.so', 'libtinyxml2-v34.so'),
    'vendor/lib64/android.hardware.audio.core-impl-mediatek.so': blob_fixup()
        .add_needed('libaudioutils_shim.so')
        .replace_needed('android.media.audio.common.types-V5-ndk.so', 'android.media.audio.common.types-V3-ndk.so')
        .replace_needed('libaudio_aidl_conversion_common_ndk.so', 'libaudio_aidl_conversion_common_ndk_prebuilt.so'),
    'vendor/lib64/libaudio_aidl_conversion_common_ndk_prebuilt.so': blob_fixup()
        .replace_needed('android.media.audio.common.types-V5-ndk.so', 'android.media.audio.common.types-V3-ndk.so'),
    'vendor/lib64/hw/android.hardware.soundtrigger3-impl.so': blob_fixup()
        .replace_needed('libaudio_aidl_conversion_common_ndk.so', 'libaudio_aidl_conversion_common_ndk_prebuilt.so'),
    'vendor/lib64/libaudio_aidl_conversion_common_ndk_prebuilt.so': blob_fixup()
        .replace_needed('android.media.audio.common.types-V5-ndk.so', 'android.media.audio.common.types-V3-ndk.so'),
    (
         'vendor/lib64/soundfx/libswdapaidl.so',
         'vendor/lib64/soundfx/libswgamedapaidl.so',
         'vendor/lib64/soundfx/libswspatializeraidl.so'
    ): blob_fixup()
        .replace_needed('android.media.audio.common.types-V5-ndk.so', 'android.media.audio.common.types-V3-ndk.so')
        .replace_needed('libaudio_aidl_conversion_common_ndk.so', 'libaudio_aidl_conversion_common_ndk_prebuilt.so'),
    'vendor/lib64/soundfx/libdlbvolaidl.so': blob_fixup()
        .replace_needed('android.media.audio.common.types-V5-ndk.so', 'android.media.audio.common.types-V3-ndk.so'),
    'system_ext/bin/hw/android.hardware.audio.parameter_parser.service': blob_fixup()
        .replace_needed('av-audio-types-aidl-ndk.so', 'av-audio-types-aidl-V3-ndk.so'),
    (
        'odm/lib64/libmituning_datacenter_base.so',
        'odm/lib64/libmituning_utils.so',
        'odm/lib64/libmituning_datacenter.so',
        'vendor/lib64/libcom.xiaomi.dsac.so',
        'vendor/lib64/com.xiaomi.immunesystem.client.so',
        'vendor/lib64/com.xiaomi.immunesystem.core.so',
        'odm/lib64/camera/components/libawbcore.so',
        'odm/lib64/camera/components/liblsccore.so',
        'odm/lib64/camera/components/libstats.afalgo.so',
        'odm/lib64/camera/components/libmiaec.so',
    ): blob_fixup()
        .replace_needed('libprotobuf-cpp-full-21.12.so', 'libprotobuf-cpp-full-21.7.so'),
    'system_ext/lib64/libimsma.so': blob_fixup()
        .replace_needed('libsink.so', 'libsink-mtk.so'),
    'system_ext/priv-app/ImsService/ImsService.apk': blob_fixup()
        .apktool_patch('blob-patches/ImsService'),
}  # fmt: skip

module = ExtractUtilsModule(
    'klee',
    'xiaomi',
    blob_fixups=blob_fixups,
    lib_fixups=lib_fixups,
    namespace_imports=namespace_imports,
    add_firmware_proprietary_file=True,
)

if __name__ == '__main__':
    utils = ExtractUtils.device(module)
    utils.run()
