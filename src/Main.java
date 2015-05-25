import org.lwjgl.BufferUtils;
import org.lwjgl.Sys;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.ByteBuffer;
 
import java.nio.FloatBuffer;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryUtil.*;
 
public class Main {
 
    // We need to strongly reference callback instances.
    private GLFWErrorCallback errorCallback;
    private GLFWKeyCallback   keyCallback;
 
    // The window handle
    private long window;
 
    public void run() {
        System.out.println("Hello LWJGL " + Sys.getVersion() + "!");
 
        try {
        	initGLFW();
            loop();
 
            // Release window and window callbacks
            glfwDestroyWindow(window);
            keyCallback.release();
        } finally {
            // Terminate GLFW and release the GLFWerrorfun
            glfwTerminate();
            errorCallback.release();
        }
    }
 
    private void initGLFW() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        glfwSetErrorCallback(errorCallback = errorCallbackPrint(System.err));
 
        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if ( glfwInit() != GL11.GL_TRUE )
            throw new IllegalStateException("Unable to initialize GLFW");
 
        // Configure our window
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GL_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GL_TRUE); // the window will be resizable
 
        int WIDTH = 1280;
        int HEIGHT = 720;
 
        // Create the window
        window = glfwCreateWindow(WIDTH, HEIGHT, "Hello World!", NULL, NULL);
        if ( window == NULL )
            throw new RuntimeException("Failed to create the GLFW window");
 
        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
                    glfwSetWindowShouldClose(window, GL_TRUE); // We will detect this in our rendering loop
            }
        });
 
        // Get the resolution of the primary monitor
        ByteBuffer vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        // Center our window
        glfwSetWindowPos(
            window,
            (GLFWvidmode.width(vidmode) - WIDTH) / 2,
            (GLFWvidmode.height(vidmode) - HEIGHT) / 2
        );
 
        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync
        glfwSwapInterval(1);
 
        // Make the window visible
        glfwShowWindow(window);
    }
 
    private void loop() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the ContextCapabilities instance and makes the OpenGL
        // bindings available for use.
        GLContext.createFromCurrent();
        
        glViewport(0,0, 1280, 720);
        
        
        //Load and compile shaders
        int programId = glCreateProgram();
        int vertexShader = loadAndCompileShader("vertexShader.vert", GL_VERTEX_SHADER);
        int fragmentShader = loadAndCompileShader("fragmentShader.frag", GL_FRAGMENT_SHADER);
        glAttachShader(programId, vertexShader);
		glAttachShader(programId, fragmentShader);
		glLinkProgram(programId);
		
		//init opengl shaders and other states
        float vertices[] = {
		    -0.5f, -0.5f, 1f,0f,0f,1f,
		     0.5f, -0.5f, 0f,1f,0f,1f,
		     0.0f,  0.5f, 0f,0f,1f,1f
		};  
        
        FloatBuffer verticesBuffer = BufferUtils.createFloatBuffer(vertices.length);
        verticesBuffer.put(vertices).flip();
        
        //VAO
        int VAO = glGenVertexArrays();
        glBindVertexArray(VAO);
        
        //VBO for vertices position and color
        int bytesOfFloat = Float.SIZE/Byte.SIZE;
        int stride = 6*bytesOfFloat;
        int offsetPosition = 0; //Position starts at beginning of each vertex
        int offsetColor = 2*bytesOfFloat; //Color starts after the position(2 indices further);
        
        int VBO = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, VBO);
        glBufferData(GL_ARRAY_BUFFER, verticesBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, offsetPosition);
        glVertexAttribPointer(1, 4, GL_FLOAT, false, stride, offsetColor);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        
        glBindVertexArray(0);
        


	    //glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        
        // Set the clear color
	    glClearColor(0,0,0, 1f);
 
        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while ( glfwWindowShouldClose(window) == GL_FALSE ) {
            glfwPollEvents();
            
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
 
            glUseProgram(programId);
            glBindVertexArray(VAO);
            glDrawArrays(GL_TRIANGLES, 0, 3);
            glBindVertexArray(0);  
            glUseProgram(0);
            glfwSwapBuffers(window); // swap the color buffers
            // Poll for window events. The key callback above will only be
            // invoked during this call.
        }
    }
    
    private int loadAndCompileShader(String filename, int shaderType)
    {
		//vertShader will be non zero if succefully created
		int handle = glCreateShader(shaderType);
 
		// load code from file into String
		String code = loadFile(filename);
 
		// upload code to OpenGL and associate code with shader
		glShaderSource(handle, code);
 
		// compile source code into binary
		glCompileShader(handle);
 
		// acquire compilation status
		int shaderStatus = glGetShaderi(handle, GL_COMPILE_STATUS);
 
		// check whether compilation was successful
		if( shaderStatus == GL11.GL_FALSE)
		{
			throw new IllegalStateException("compilation error for shader ["+filename+"]. Reason: " + glGetShaderInfoLog(handle, 1000));
		}
 
		return handle;
    }
 
	/**
	 * Load a text file and return its contents as a String.
	 */
	private String loadFile(String filename)
	{
		StringBuilder vertexCode = new StringBuilder();
		String line = null ;
		try
		{
		    BufferedReader reader = new BufferedReader(new FileReader(filename));
		    while( (line = reader.readLine()) !=null )
		    {
		    	vertexCode.append(line);
		    	vertexCode.append('\n');
		    }
		}
		catch(Exception e)
		{
			throw new IllegalArgumentException("unable to load shader from file ["+filename+"]", e);
		}
 
		return vertexCode.toString();
	}
 
    public static void main(String[] args) {
        new Main().run();
    }
 
}