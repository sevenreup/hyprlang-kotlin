cmake_minimum_required(VERSION 3.22.1)

project("hyprlang-wrapper")

# Set C++ Standard
set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Define hyprutils (String only)
# We need to include the headers
include_directories(
    "${CMAKE_CURRENT_SOURCE_DIR}/hyprutils/include"
    "${CMAKE_CURRENT_SOURCE_DIR}/hyprlang/include"
    "${CMAKE_CURRENT_SOURCE_DIR}/hyprlang/src"
)

# Sources for hyprutils (only string module to avoid pixman dependency)
file(GLOB HYPRUTILS_STRING_SOURCES
    "${CMAKE_CURRENT_SOURCE_DIR}/hyprutils/src/string/*.cpp"
)

add_library(hyprutils_core STATIC
    ${HYPRUTILS_STRING_SOURCES}
)

# Sources for hyprlang
file(GLOB HYPRLANG_SOURCES
    "${CMAKE_CURRENT_SOURCE_DIR}/hyprlang/src/*.cpp"
)

add_library(hyprlang_lib STATIC
    ${HYPRLANG_SOURCES}
)

target_compile_features(hyprlang_lib PUBLIC cxx_std_23)
target_compile_definitions(hyprlang_lib PRIVATE HYPRLANG_INTERNAL)
target_link_libraries(hyprlang_lib PUBLIC hyprutils_core)

# Helper to suppress warnings from 3rd party code if needed
target_compile_options(hyprlang_lib PRIVATE -w)
target_compile_options(hyprutils_core PRIVATE -w)

# JNI Wrapper
add_library(hyprlang_wrapper SHARED
    hyprlang_wrapper.cpp
)

target_link_libraries(hyprlang_wrapper
    PRIVATE
    hyprlang_lib
    hyprutils_core
    android
    log
)
