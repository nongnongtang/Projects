package weka.classifiers.meta;

import weka.classifiers.*;
import weka.core.*;

//import java.util.Enumeration;
//import java.util.Random;
//import java.util.Vector;
import java.util.*;

import weka.filters.unsupervised.instance.RemoveDuplicates;
import weka.clusterers.SimpleKMeans;

public class MyBagging extends RandomizableParallelIteratedSingleClassifierEnhancer
{

  /** for serialization */
  static final long serialVersionUID = -50587962237199703L;
  
  /**
   * Constructor.
   */
  public MyBagging() {
    
    m_Classifier = new weka.classifiers.trees.REPTree();
  }
  
  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
 
    return "Class for bagging a classifier to reduce variance. Can do classification "
	+ "and regression depending on the base learner. \n\n";
     
  }




  /**
   * Creates a new dataset of the same size using random sampling
   * with replacement according to the given weight vector. The
   * weights of the instances in the new dataset are set to one.
   * The length of the weight vector has to be the same as the
   * number of instances in the dataset, and all weights have to
   * be positive.
   *
   * @param data the data to be sampled from
   * @param random a random number generator
   * @param sampled indicating which instance has been sampled
   * @return the new dataset
   * @throws IllegalArgumentException if the weights array is of the wrong
   * length or contains negative weights.
   */
  public Instances bootstrapSample(Instances data, Random random) {
    Instances sample = new Instances( data, 0);
    int n = data.size();
    for (int i = 0; i < n ; i++) {
      int index = random.nextInt( n );
      Instance next = data.get(index);
      sample.add ( next );
    }
    return sample;
  }



  // get validation set
  /*
  public Instances validationSample(Instances data, Random random){
	
	Instances resample = new Instances( data, 0);
    int n = data.size();
    for (int i = 0; i < n ; i++) {
      int index = random.nextInt( n );
      Instance next = data.get(index);
      resample.add ( next );
    }
	Instances noDuplictaes = resample.RemoveDuplicates();
	Instances validation = data;
	for (int j = 0; j < noDuplictaes.length; j++){
	  Instance now = noDuplictaes.get(j);
	  validation.delete(now);
	}
	return validation;
	
  }
  */

  
  
  
  protected Instances m_data;

  
  /**
   * Returns a training set for a particular iteration.
   * 
   * @param iteration the number of the iteration for the requested training set.
   * @return the training set for the supplied iteration number
   * @throws Exception if something goes wrong when generating a training set.
   */
  protected synchronized Instances getTrainingSet(int iteration) throws Exception {
    Random r = new Random(m_Seed + iteration);
    return bootstrapSample( m_data, r);
  }
  
  /**
   * Bagging method.
   *
   * @param data the training data to be used for generating the
   * bagged classifier.
   * @throws Exception if the classifier could not be built successfully
   */
  public void buildClassifier(Instances data) throws Exception {

    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    // remove instances with missing class
    m_data = new Instances(data);
    m_data.deleteWithMissingClass();
    
    super.buildClassifier(m_data);

    Random random = new Random(m_Seed); // ? seed
    
    for (int j = 0; j < m_Classifiers.length; j++) {      // ? m_Classifiers
      if (m_Classifier instanceof Randomizable) {
	((Randomizable) m_Classifiers[j]).setSeed(random.nextInt());
      }
    }
    
    buildClassifiers(); //why ?
    
	// add a new method for clustering base classifiers
	//Instances validationSet = validationSample(m_data, random);
	//Instances probsOfClassifers = validateClassifiers(validationSet); 
    Instances probsOfClassifers = validateClassifiers(m_data); 
	simpleClustering(probsOfClassifers);
	
    // save memory
    m_data = null;
  }

  /**
   * Calculates the class membership probabilities for the given test
   * instance.
   *
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @throws Exception if distribution can't be computed successfully 
   */
  public double[] distributionForInstance(Instance instance) throws Exception {

    double [] sums = new double [instance.numClasses()]; 
    
    for (int i = 0; i < m_NumIterations; i++) { // where is m_NumIterations
      // CLASSIFICATION
      double[] newProbs = m_Classifiers[i].distributionForInstance(instance);
      for (int j = 0; j < newProbs.length; j++)
	sums[j] += newProbs[j];
    }
    Utils.normalize(sums);
    return sums;
  }

  // probabilities of each classifier on validation set
   public Instances validateClassifiers(Instances validationSet) throws Exception {

    double [][] probs = new double [m_NumIterations][validationSet.size()]; 
    
    for (int i = 0; i < m_NumIterations; i++) { 
      
      
	  // each array for one classifier
      for (int j = 0; j < validationSet.size(); j++){
		  double[] newProbs = m_Classifiers[i].distributionForInstance(validationSet.get(j));
		  probs[i][j] = newProbs[0];
	  }
	
    }
    
    ArrayList<Attribute> attributes = new ArrayList<Attribute>();
    for (int i = 0; i < validationSet.size(); i++) {
      String name = "p0_inst_" + i;
      attributes.add( new Attribute( name ));
    }

    Instances pb = new Instances("probs_per_classifier",attributes, 0);

    for (int i = 0; i < m_NumIterations; i++){
      pb.add(new DenseInstance(1.0, probs[i]));
    }
	
    return pb;
  } 
  
  // clustering
  public void simpleClustering(Instances probs) throws Exception {
		SimpleKMeans kmeans = new SimpleKMeans();
 
		kmeans.setSeed(10);
 
		//important parameter to set: preserver order, number of cluster.
		kmeans.setPreserveInstancesOrder(true);
		
		if (m_numClusters != 0){
	    kmeans.setNumClusters(m_numClusters);
		} else {
		kmeans.setNumClusters( (int) Math.sqrt(m_NumIterations));
        }
	 	kmeans.buildClusterer(probs);
 
		// This array returns the cluster number (starting with 0) for each instance (probsOfClassifer)
		// The array has as many elements as the number of instances
		int[] assignments = kmeans.getAssignments();
		Instances centroids = kmeans.getClusterCentroids();
		DistanceFunction d = kmeans.getDistanceFunction();

		int i=1;
		for(int clusterNum : assignments) {
		    System.out.printf("Tree %d -> Cluster %d \n", i, clusterNum);
		    i++;
		}

		int[] closest = new int[ kmeans.getNumClusters()];
		for (i = 0; i < closest.length; i++) {
		  closest[i] = closestPoint( i, probs, assignments, centroids.get(i), d);
		}

		Classifier[] subset = new Classifier[ closest.length ];
		for (i = 0; i < closest.length; i++) {
		  subset[ i ] = m_Classifiers[ closest[i] ];
		}
		m_Classifiers = subset;
		m_NumIterations = subset.length;
	}
  
  
  
  

  public int closestPoint(int clusterNumber, Instances probs, int[] assignments, 
                          Instance centroid, DistanceFunction d) {
    // return index of the closest point to cluster with clusterNumber 
	  
		ArrayList<Integer> index = new ArrayList<Integer>();
		Instances cluster = new  Instances(probs, 0);
	    for (int i = 0; i < assignments.length; i++){
		   if (assignments[i] == clusterNumber){
			   Instance cl = probs.get(i);
			   cluster.add(cl);
			   index.add(i);
		   }
		}
	   
		double minimum_current = d.distance(centroid, cluster.get(0));
		double minimum_later = 0;
		int index_closest = index.get(0);
		for (int j = 1; j < cluster.size(); j++){
		minimum_later = d.distance(centroid, cluster.get(j));
		  if (minimum_current > minimum_later){
			  minimum_current = minimum_later;
			  index_closest = index.get(j);
			}
			
		}
		return index_closest;
		
  }
  
  
  
  
  
  
  
  
  
  
// add a new optioner for clustering classifiers
   protected int m_numClusters = 0;
   
   public int getNumClusters() { 
              return m_numClusters; }
   
   public void setNumClusters(int k) { 
               m_numClusters = k; }

			   
   public String numClustersTipText() {
      return " Sets the number of clusters to use for selecting diverse classifier" + 
	          " default is 0, don't cluster";
    }

	
	
	
	
	public Enumeration listOptions() {

    Vector newVector = new Vector(2);

    newVector.addElement(new Option(
              "\tNumber of clusters\n" 
              + "\t(default 0)",
              "K", 1, "-K"));
    

    Enumeration enu = super.listOptions();
    while (enu.hasMoreElements()) {
      newVector.addElement(enu.nextElement());
    }
    return newVector.elements();
  }

  
  
  
    public void setOptions(String[] options) throws Exception {
 
     String cluster = Utils.getOption('K', options);
     if (cluster.length() != 0) {
       setNumClusters(Integer.parseInt(cluster));
     } else {
       setNumClusters(0);
     }
 
     super.setOptions(options);
   }
  
  
  
  
    public String [] getOptions() {

     String [] superOptions = super.getOptions();
     String [] options = new String [superOptions.length + 2];

     int current = 0;
     options[current++] = "-K";
     options[current++] = "" + getNumClusters();

     System.arraycopy(superOptions, 0, options, current,
                     superOptions.length);

     return options;
   }
   
  
  
  
  
  
  
  
  
  
  
  
  
  public String toString() {
    StringBuilder sb = new StringBuilder("MyBagging\n\n");
	if (m_Classifiers != null) {
		for (Classifier cl: m_Classifiers) {
			sb.append( cl.toString() );
		}
	}
    return sb.toString();
  }


  /*
  public String toString() {
    String s = "MyBagging\n\n";
    for (Classifier cl: m_Classifiers) {
      s += cl.toString();
    }
    return s;
  }
  */

  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {
    runClassifier(new MyBagging(), argv);
  }
}