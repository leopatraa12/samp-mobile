//
// Created by aurora on 20.10.2025.
//
#pragma once

#include <string>
struct aurora
{
    std::string szHost = "135.148.164.122";
    int iPort = 21450;
};

inline aurora Getaurora()
{
    aurora config;
    return config;
}