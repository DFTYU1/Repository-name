#include "../../platform-android/src/main/cpp/inference_trace.h"
#include <cassert>
#include <cmath>
#include <iostream>
#include <stdexcept>
int main(){
 double metrics[12]{};std::atomic<int64_t> request{7};
 try{PhaseTrace trace(metrics,request);trace.next(7);trace.next(8);metrics[1]=123;request=0;throw std::runtime_error("cancel");}catch(const std::runtime_error&){}
 assert(metrics[1]==123 && metrics[9]==8 && metrics[10]==3 && metrics[2]>=0 && std::isfinite(metrics[3]));
 request=8;{PhaseTrace trace(metrics,request);assert(metrics[1]==0 && metrics[10]==0);trace.next(5);trace.next(6);trace.next(7);trace.next(8);metrics[1]=5;metrics[10]=1;}
 assert(metrics[10]==1 && metrics[1]==5);double sum=0;for(int i=4;i<=8;i++)sum+=metrics[i];assert(std::abs(metrics[2]-sum)<2);
 {PhaseTrace trace(metrics,request);trace.next(7);}assert(metrics[10]==4 && metrics[9]==7);
 {PhaseTrace trace(metrics,request);trace.next(8);metrics[10]=2;}assert(metrics[10]==2);
 std::cout<<"PASS cancellation unwinding, request reset, EOG, budget, error phase, stage totals (host only)\n";
}
