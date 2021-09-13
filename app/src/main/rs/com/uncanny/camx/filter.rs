#pragma version(1)
#pragma rs java_package_name(com.uncanny.camx)

uchar4 RS_KERNEL root(uchar4 in, int32_t x, int32_t y){
    uchar4 out;
    out.a = in.a;
    out.r = 0xFF - in.r;
    out.g = 0xFF - in.g;
    out.b = 0xFF - in.b;
    return out;
}