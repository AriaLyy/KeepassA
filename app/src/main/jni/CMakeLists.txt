cmake_minimum_required(VERSION 3.4.1)

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