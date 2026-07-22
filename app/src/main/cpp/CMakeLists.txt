cmake_minimum_required(VERSION 3.22.1)
project("omnifiles_native")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -O3 -flto")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O3 -flto")

# Create shared library
add_library(
    native_lister
    SHARED
    native_lister.cpp
)

# Find system libraries
find_library(log-lib log)

# Link libraries
target_link_libraries(
    native_lister
    ${log-lib}
)
