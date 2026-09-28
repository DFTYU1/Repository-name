#pragma once
#include <algorithm>
#include <atomic>
#include <chrono>
#include <cstdint>
#include <iterator>
struct PhaseTrace {
    double (&stats)[12];
    const std::atomic<int64_t>& active_request;
    std::chrono::steady_clock::time_point started=std::chrono::steady_clock::now(), phase_started=started;
    int phase=4;
    PhaseTrace(double (&values)[12],const std::atomic<int64_t>& request):stats(values),active_request(request){std::fill(std::begin(stats),std::end(stats),0.0);stats[9]=phase;}
    void finish_phase(){stats[phase]+=std::chrono::duration<double,std::milli>(std::chrono::steady_clock::now()-phase_started).count();}
    void next(int value){finish_phase();phase=value;phase_started=std::chrono::steady_clock::now();stats[9]=phase;}
    ~PhaseTrace(){finish_phase();stats[2]=std::chrono::duration<double,std::milli>(std::chrono::steady_clock::now()-started).count();
        stats[3]=stats[1]*1000.0/std::max(1.0,stats[8]);
        if(stats[10]==0)stats[10]=active_request.load()==0?3:4;}
};
