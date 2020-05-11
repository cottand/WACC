# WACC

WACC is a toy compiler that supports basic constructs like stack allocated primitives, and heap-allocated arrays and pairs.

Here is an exmaple:
```
begin
  char continue = 'Y' ;
  int buff = 0 ;
  while continue != 'N' do
    print "Please input an integer: " ;
    read buff ;
    print "echo input: " ;
    println buff ;
    println "Do you want to continue entering input?" ; 
    println "(enter Y for \'yes\' and N for \'no\')" ;
    read continue
  done
end
```
It compiles to both ARM11 and the JVM, and is thus compatible with Java and offers some basic interop with it.

Team members and contributors to this project were:
- [Willam Profit](https://github.com/williamprofit)
- [Lancelot Blanchard](https://www.linkedin.com/in/lancelotblanchard)
- [Kacper Kazaniecki](https://www.linkedin.com/in/kacperkazaniecki)
- [Nico D'Cotta](https://www.linkedin.com/in/ndcotta)
