//
// Created by x1y2z on 21.09.2023.
//

#ifndef BKUZN_RQVECTOR_H
#define BKUZN_RQVECTOR_H


class RQVector {

    union {
        struct {
            float x;
            float y;
            float z;
            float w;
        };
        struct {
            float r;
            float g;
            float b;
            float a;
        };
    };
};


#endif //BKUZN_RQVECTOR_H
