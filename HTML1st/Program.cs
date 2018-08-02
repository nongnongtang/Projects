using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using HtmlAgilityPack;
using System.Net;
using System.Collections;
using Microsoft.CSharp;
using System.CodeDom.Compiler;
using System.Reflection;
using System.IO;
using System.Text.RegularExpressions;
using System.Web;
using System.Xml;

namespace Final
{
    class Program
    {
        static void Main(string[] args)
        {

            //HtmlDocument html = new HtmlDocument();
            //html.Load("sampleInputHtml.html");
            //var root = html.DocumentNode;
            //var commonPosts = root.Descendants().Where(n => n.GetAttributeValue("class", "").Equals("html1st"));

            //Parsing sampleInput Html 
            XmlDocument doc = new XmlDocument();
            doc.Load("Test3.html");
            XmlNode root = doc.DocumentElement;
            XmlNodeList commonPosts = root.SelectNodes("//div[@class = 'bool']");
            //Console.WriteLine(commonPosts.Count);
            ArrayList list = new ArrayList();

            foreach (XmlNode a in commonPosts)
            {
                string b = WebUtility.HtmlDecode(a.InnerText);
                

                list.Add(b);

                //foreach (var i in list)
                //{
                  //  Console.WriteLine(i);
               // }
                //Console.WriteLine("{0}", b);
               

            }

            //create a new instance of the c# compiler
            CSharpCodeProvider compiler = new CSharpCodeProvider();
            //ICodeCompiler compiler = codeProvider.CreateCompiler();
           
            // Create some parameters for the compiler
            CompilerParameters parameters = new CompilerParameters();
            parameters.GenerateExecutable = false;
            parameters.GenerateInMemory = true;

            parameters.ReferencedAssemblies.Add("System.dll");
            parameters.ReferencedAssemblies.Add("MathLibrary.DLL");
            var results = compiler.CompileAssemblyFromFile(parameters, "MyOwnMethods.cs");

          

            if (results.Errors.Count == 0)
            {
                ArrayList replace = new ArrayList();
                int count = 0;
                foreach (string i in list)
                {
                    //int n = i.IndexOf('(');
                    int p = i.IndexOf('(');
                    int q = i.IndexOf(')');
                    string nameOfMethod = i.Substring(0, p);
                    //Console.WriteLine(nameOfMethod);

                    string b = i.Substring(p + 1, q - p - 1);
                    string words = b.Replace("\"", "");
                    string[] parameter = words.Split(',');

                    //Console.WriteLine(parameter);
                    if (parameter.Length <= 1)
                    {
                        var myMehtod = results.CompiledAssembly.CreateInstance("MyMethods");
                        var a = myMehtod.GetType().
                              GetMethod(nameOfMethod).
                              Invoke(myMehtod, new[] { parameter[0] });
                        Console.WriteLine(a);
                        replace.Add(a);
                    }
                    if (parameter.Length > 1)
                    {
                        var math = results.CompiledAssembly.CreateInstance("MyMethods");
                        var par = math.GetType().
                            GetMethod(nameOfMethod).
                            Invoke(math, new[] { parameter });
                        Console.WriteLine(par);
                        replace.Add(par);
                    }

                }
                    using (StreamReader stream = new StreamReader("Test3.html"))
                    using (StreamWriter writeStream = new StreamWriter("sampleOutputHTML.html"))
                    {
                        string line;

                      

                        
                        while ((line = stream.ReadLine())!= null)
                        {
                            writeStream.WriteLine(line);
                        }
                    }

                //HtmlDocument html2 = new HtmlDocument();
                //html2.Load("sampleOutputHTML.html");
                //var root2 = html2.DocumentNode;
                //var countNum = root2.SelectNodes("//div[@class = 'html1st']//text()");

                //Parsing sampleOutput Html which is copied from sampleInput Html 
                XmlDocument doc2 = new XmlDocument();
                doc2.Load("sampleOutputHTML.html");
                XmlNode root2 = doc2.DocumentElement;
                XmlNodeList countNum = root2.SelectNodes("//div[@class = 'bool']");
                Console.WriteLine(countNum.Count);
                /* for (int i=0; i<countNum.Count();i++) { 
                 var commonPosts2 = root2.SelectSingleNode("//div[@class = 'html1st']//text()") as HtmlTextNode;
                     // Console.WriteLine(commonPosts2.Count());

                     //foreach (var htmlNode in commonPosts2)
                     // {

                     //string inner =  htmlNode.InnerText;
                     // string newInner = Regex.Replace((string)htmlNode, inner, replace[0]);
                     // htmlNode.ParentNode.ReplaceChild(HtmlTextNode.CreateNode(htmlNode.InnerText + "_translated"), htmlNode);

                     commonPosts2.Text = (string)replace[i];
                     Console.WriteLine(commonPosts2.InnerText);
                     //Console.WriteLine(commonPosts2.Text);

                 }
                 */

                /*foreach (var htmlNode in countNum)
                {
                    var newNodeStr = replace[count];
                    Console.WriteLine(newNodeStr);
                    var newNode = HtmlNode.CreateNode( ""+newNodeStr+"" );
                    htmlNode.ParentNode.ReplaceChild(newNode, htmlNode);
                    count++;
                    html2.Save("sampleOutputHTML.html");
                } */

                foreach (XmlNode XmlNode in countNum)
                {

                    //var newNodeStr = replace[count];
                    //Console.WriteLine(newNodeStr);
                    XmlAttribute attribute = XmlNode.Attributes[0];
                    XmlElement newNode = doc2.CreateElement("div");
                    newNode.SetAttribute(attribute.Name,attribute.Value);
                    newNode.InnerText = (string)replace[count]; 
                    XmlNode.ParentNode.ReplaceChild(newNode, XmlNode);
                    count++;
                    doc2.Save("sampleOutputHTML.html");
                } 
            }

                //var myMehtods = results.CompiledAssembly.CreateInstance("MyMethods");
                //myMehtods.GetType().
                //    GetMethod("GetGreeting").
                //   Invoke(myMehtods, new[] { " Boyang " });

                //var math = results.CompiledAssembly.CreateInstance("MyMethods");
                //math.GetType().
                //    GetMethod("GetNumber").
                //  Invoke(math, new[] { "12", "13" });
            
            else
            {
                var temp = results.Errors;
                foreach (System.CodeDom.Compiler.CompilerError e in temp)
                {
                    System.Console.WriteLine(e);
                }
            }



            Console.Read();


        }
    }
}
