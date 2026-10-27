// Independently authored bounded token protocol; no text or tokens are logged.
#include <ctranslate2/translator.h>
#include <iostream>
#include <cstdint>
#include <stdexcept>
static uint32_t read32(){uint32_t n=0;for(int i=0;i<4;++i){int b=std::cin.get();if(b<0)throw std::runtime_error("input");n|=uint32_t(b)<<(8*i);}return n;}
static void write32(uint32_t n){for(int i=0;i<4;++i)std::cout.put((n>>(8*i))&255);}
int main(){try{
 uint32_t count=read32();if(count<2||count>128)throw std::runtime_error("count");
 std::vector<std::string> input;
 for(uint32_t i=0;i<count;++i){auto n=read32();if(n<1||n>1024)throw std::runtime_error("length");std::string s(n,'\0');std::cin.read(s.data(),n);if(!std::cin||s.find('\0')!=std::string::npos)throw std::runtime_error("token");input.push_back(s);}
 if(std::cin.get()!=EOF||input.back()!="</s>")throw std::runtime_error("trailing");
 ctranslate2::ReplicaPoolConfig config;config.num_threads_per_replica=2;config.max_queued_batches=1;
 ctranslate2::Translator translator("/model",ctranslate2::Device::CPU,ctranslate2::ComputeType::FLOAT32,{0},false,config);
 auto model=translator.get_first_replica().model();
 if(model->device()!=ctranslate2::Device::CPU||model->effective_compute_type()!=ctranslate2::ComputeType::FLOAT32)throw std::runtime_error("backend");
 ctranslate2::TranslationOptions o;o.beam_size=4;o.patience=1;o.length_penalty=1;o.max_input_length=128;o.max_decoding_length=32;o.min_decoding_length=1;o.disable_unk=true;o.replace_unknowns=false;o.return_end_token=true;
 auto results=translator.translate_batch({input},o);auto tokens=results.at(0).hypotheses.at(0);
 if(tokens.empty()||tokens.size()>32)throw std::runtime_error("output");
 write32(tokens.size());for(const auto& s:tokens){if(s.empty()||s.size()>1024)throw std::runtime_error("output");write32(s.size());std::cout.write(s.data(),s.size());}
 return std::cout?0:1;
 }catch(...){std::cerr<<"CPU translation unavailable\n";return 1;}}
