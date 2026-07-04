cmake_minimum_required(VERSION 3.4.1)

# 设置编译器标志（确保内存访问符合 16KB 对齐）
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -mstrict-align -mno-unaligned-access")
# 设置链接器标志（强制 16KB 页面对齐）
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")

add_library(
        keepassA SHARED
        encrypt_str.cpp
        aes/aes_cbc.c
        aes/aes_core.c
        aes/aes_ecb.c
        aes/cbc128.c
)

#find_library(
#        aesUtil
#        final_key
#)