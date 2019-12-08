# coassign

Solves maximum-weight bipartite b-matching problem using cost-scaling method.

For a bipartite graph ![bipartite](https://latex.codecogs.com/gif.latex?%5Cinline%20G%3D%28U%20%5Csqcup%20V%2C%20E%29), weight ![weight](https://latex.codecogs.com/gif.latex?%5Cinline%20w%3A%20E%20%5Cto%20%5Cmathbb%7BZ%7D_%7B%3E%200%7D) and multiplicity ![multiplicity](https://latex.codecogs.com/gif.latex?%5Cinline%20b%20%3A%20U%20%5Csqcup%20V%20%5Cto%20%5Cmathbb%7BZ%7D_%7B%3E0%7D), maximum-weight bipartite b-matching problem is defined as follows;   

![problem](https://latex.codecogs.com/gif.latex?%5Cbegin%7Bmatrix%7D%20%5Cmax.%20%26%5Csum_%7Be%20%5Cin%20E%7D%20w_e%20x_e%20%5C%5C%20%5Cmathrm%7Bs.t.%7D%20%26%20%5Csum_%7Bu%20%5Cin%20U%7D%20x_%7Bu%2C%20v%7D%20%5Cle%20b_v%20%5C%5C%20%26%20%5Csum_%7Bv%20%5Cin%20V%7D%20x_%7Bu%2C%20v%7D%20%5Cle%20b_u%20%5C%5C%20%26%20x_e%20%5Cin%20%5C%7B0%2C%201%5C%7D%20%5Cend%7Bmatrix%7D)

This library is to solve the above problem using cost-scaling algorithm.
