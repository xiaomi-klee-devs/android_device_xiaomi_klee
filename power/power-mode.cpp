/*
 * Copyright (C) 2021 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#include <aidl/android/hardware/power/BnPower.h>
#include <android-base/file.h>
#include <android-base/logging.h>
#include <sys/ioctl.h>
#include <fcntl.h>
#include <unistd.h>
#include <cstring>

// Touch device constants
#define TOUCH_DEV_PATH "/dev/xiaomi-touch"
#define TOUCH_ID 0

// IOCTL commands
#define IOCTL_SET_TOUCH_ID 0x40005403
#define IOCTL_TOUCH_OPERATION 0xc4085400

// Touch modes
#define Touch_Doubletap_Mode 14

// Touch command types
#define CMD_SET_CUR_VALUE 0

namespace aidl {
namespace google {
namespace hardware {
namespace power {
namespace impl {
namespace pixel {

using ::aidl::android::hardware::power::Mode;

namespace {

// Touch ioctl data structure
struct CommonDataPacket {
    uint8_t touchId;
    uint8_t command;
    uint16_t mode;
    uint16_t length;
    uint16_t reserved;
    int buffer[256];
    CommonDataPacket() : touchId(0), command(0), mode(0),
                         length(0), reserved(0) {
        memset(buffer, 0, sizeof(buffer));
    }
};

// Global touch device file descriptor
static int touchDeviceFd = -1;

/**
 * Initialize touch device for power mode operations
 */
static bool initTouchDevice() {
    if (touchDeviceFd >= 0) {
        return true;  // Already initialized
    }
    touchDeviceFd = open(TOUCH_DEV_PATH, O_RDWR);
    if (touchDeviceFd < 0) {
        LOG(ERROR) << "Failed to open touch device: " << strerror(errno);
        return false;
    }
    // Set touch device ID
    int result = ioctl(touchDeviceFd, IOCTL_SET_TOUCH_ID, (unsigned long)TOUCH_ID);
    if (result < 0) {
        LOG(ERROR) << "Failed to set touch ID: " << strerror(errno);
        close(touchDeviceFd);
        touchDeviceFd = -1;
        return false;
    }
    LOG(DEBUG) << "Touch device initialized for power modes";
    return true;
}

/**
 * Set touch mode using proper xiaomi_touch_ioctl structure
 */
static bool setTouchMode(int mode, int value) {
    if (!initTouchDevice()) {
        LOG(ERROR) << "Touch device not initialized";
        return false;
    }
    CommonDataPacket packet;
    packet.touchId = TOUCH_ID;
    packet.command = CMD_SET_CUR_VALUE;
    packet.mode = mode;
    packet.length = 1;
    packet.buffer[0] = value;
    ioctl(touchDeviceFd, IOCTL_TOUCH_OPERATION, &packet);
    return true;
}

}  // anonymous namespace

bool isDeviceSpecificModeSupported(Mode type, bool* _aidl_return) {
    switch (type) {
        case Mode::DOUBLE_TAP_TO_WAKE:
            *_aidl_return = true;
            return true;
        default:
            return false;
    }
}

bool setDeviceSpecificMode(Mode type, bool enabled) {
    switch (type) {
        case Mode::DOUBLE_TAP_TO_WAKE: {
            return setTouchMode(Touch_Doubletap_Mode, enabled ? 1 : 0);
        }
        default:
            return false;
    }
}

}  // namespace pixel
}  // namespace impl
}  // namespace power
}  // namespace hardware
}  // namespace google
}  // namespace aidl