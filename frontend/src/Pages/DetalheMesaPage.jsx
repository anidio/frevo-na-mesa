// src/Pages/DetalheMesaPage.jsx

import React, { useState, useEffect, useMemo } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import apiClient from '../services/apiClient'; // USANDO O API CLIENT

const categorias = ['Todos', 'Entradas', 'Pratos', 'Sobremesas', 'Bebidas'];

// --- COMPONENTE HEADER ---
const DetalheMesaHeader = ({ mesa, onMesaUpdate }) => {
  const navigate = useNavigate();
  const [nomeCliente, setNomeCliente] = useState('');
  
  useEffect(() => {
    setNomeCliente(mesa.nomeCliente || '');
  }, [mesa]);

  const handleSalvarNome = async () => {
    if (nomeCliente !== (mesa.nomeCliente || '')) {
      try {
        await apiClient.patch(`/api/mesas/${mesa.id}/cliente`, { nomeCliente });
        toast.success("Nome do cliente salvo!");
        onMesaUpdate();
      } catch (error) {
        toast.error("Erro de comunicação ao salvar nome.");
      }
    }
  };
  
  const handleActionClick = async () => {
    let novoStatus;
    if (mesa.status === 'LIVRE') novoStatus = 'OCUPADA';
    else if (mesa.status === 'PAGA') novoStatus = 'LIVRE';
    else return;

    try {
      await apiClient.patch(`/api/mesas/${mesa.id}/status`, { status: novoStatus });
      toast.success(`Mesa marcada como ${novoStatus}!`);
      if (novoStatus === 'LIVRE') navigate('/mesas');
      else onMesaUpdate();
    } catch (error) {
      toast.error('Erro de comunicação com o servidor.');
    }
  };

  const renderActionButton = () => {
    switch (mesa.status) {
      case 'LIVRE': return <button onClick={handleActionClick} className="px-4 py-2 rounded-lg font-semibold border transition-colors bg-green-600 text-white hover:bg-green-700">Marcar como Ocupada</button>;
      case 'PAGA': return <button onClick={handleActionClick} className="px-4 py-2 rounded-lg font-semibold border transition-colors bg-blue-600 text-white hover:bg-blue-700">Liberar Mesa</button>;
      default: return null;
    }
  };

  const statusTagClasses = {
    OCUPADA: 'bg-orange-100 text-orange-800 border-orange-300',
    LIVRE: 'bg-green-100 text-green-800 border-green-300',
    PAGA: 'bg-blue-100 text-blue-800 border-blue-300',
  };

  return (
    <div className="flex justify-between items-center mb-6 pb-6 border-b">
      <div className="flex items-center gap-4 flex-wrap">
        <Link to="/mesas" className="flex items-center gap-2 text-gray-600 hover:text-gray-900">
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M10 19l-7-7m0 0l7-7m-7 7h18" /></svg>
          <span className="font-semibold">Voltar</span>
        </Link>
        <div className="h-8 border-l border-gray-300"></div>
        <div className="flex items-baseline gap-2">
          <h1 className="text-3xl font-bold text-gray-800">Mesa {mesa.numero}</h1>
          <input type="text" placeholder="Nome do Cliente" className="text-lg font-semibold text-gray-600 border-b-2 border-transparent focus:border-orange-500 bg-transparent outline-none transition-colors w-48" value={nomeCliente} onChange={(e) => setNomeCliente(e.target.value)} onBlur={handleSalvarNome} />
        </div>
        <span className={`px-3 py-1 text-sm font-bold rounded-md border ${statusTagClasses[mesa.status]}`}>{mesa.status}</span>
      </div>
      <div>{renderActionButton()}</div>
    </div>
  );
};

// --- COMPONENTE PRODUTOCARD ---
const ProdutoCard = ({ produto, onAdicionar }) => {
  const [quantidade, setQuantidade] = useState(1);
  const handleDecrease = () => setQuantidade(q => Math.max(1, q - 1));
  const handleIncrease = () => setQuantidade(q => q + 1);
  const categoriaStyles = { Entradas: 'bg-green-100 text-green-800', Pratos: 'bg-red-100 text-red-800', Sobremesas: 'bg-purple-100 text-purple-800', Bebidas: 'bg-blue-100 text-blue-800' };
  return (
    <div className="bg-white p-4 rounded-lg shadow-md flex flex-col justify-between">
      <div>
        <span className={`text-xs font-semibold px-2 py-1 rounded-full ${categoriaStyles[produto.categoria] || 'bg-gray-100 text-gray-800'}`}>{produto.categoria}</span>
        <h3 className="font-bold text-lg text-gray-800 mt-2">{produto.nome}</h3>
        <p className="text-sm text-gray-500 h-10">{produto.descricao}</p>
      </div>
      <div className="flex justify-between items-center mt-4">
        <p className="text-xl font-semibold text-gray-900">R$ {produto.preco.toFixed(2).replace('.', ',')}</p>
        <div className="flex items-center gap-2">
          <button onClick={handleDecrease} className="bg-gray-200 rounded-full w-8 h-8 font-bold text-lg flex items-center justify-center">-</button>
          <span className="w-8 text-center font-bold">{quantidade}</span>
          <button onClick={handleIncrease} className="bg-gray-200 rounded-full w-8 h-8 font-bold text-lg flex items-center justify-center">+</button>
        </div>
        <button onClick={() => onAdicionar(produto, quantidade)} className="bg-orange-500 text-white font-bold py-2 px-3 rounded-lg hover:bg-orange-600 transition-colors">Adicionar</button>
      </div>
    </div>
  );
};

// --- COMPONENTE PRINCIPAL DA PÁGINA ---
const DetalheMesaPage = () => {
  const [mesa, setMesa] = useState(null);
  const [cardapio, setCardapio] = useState([]);
  const [pedido, setPedido] = useState([]);
  const [filtroCategoria, setFiltroCategoria] = useState('Todos');
  const [termoBusca, setTermoBusca] = useState('');
  const { id } = useParams();

  const fetchMesaEProdutos = async () => {
    try {
      // Usando Promise.all para buscar ambos em paralelo
      const [mesaData, cardapioData] = await Promise.all([
        apiClient.get(`/api/mesas/${id}`),
        apiClient.get('/api/produtos')
      ]);
      setMesa(mesaData);
      setCardapio(cardapioData);
    } catch (error) {
      console.error("Erro ao buscar dados:", error);
      toast.error("Não foi possível carregar os dados da mesa ou do cardápio.");
      setMesa(null);
    }
  };

  useEffect(() => {
    fetchMesaEProdutos();
  }, [id]);
  
  // Adicione esta função após as outras funções do componente
  const handlePrint = () => {
  // Captura os dados do pedido para impressão
  const conteudoImpressao = `
    <html>
    <head>
      <title>Pedido Mesa ${mesa?.numero || ''}</title>
      <style>
        body {
          font-family: monospace;
          font-size: 12px;
          width: 270px;
          margin: 0;
          padding: 10px;
        }
        .header {
          text-align: center;
          font-weight: bold;
          margin-bottom: 10px;
          font-size: 14px;
        }
        .item {
          margin-bottom: 5px;
        }
        .obs {
          font-style: italic;
          font-size: 10px;
          margin-left: 10px;
        }
        .divider {
          border-top: 1px dashed #000;
          margin: 10px 0;
        }
      </style>
    </head>
    <body>
      <div class="header">
        PEDIDO - MESA ${mesa?.numero || ''}
        ${mesa?.nomeCliente ? `<br>Cliente: ${mesa.nomeCliente}` : ''}
        <br>${new Date().toLocaleString()}
      </div>
      <div class="divider"></div>
      ${pedido.map(item => `
        <div class="item">
          <strong>${item.quantidade}x ${item.nome}</strong>
          ${item.observacao ? `<div class="obs">Obs: ${item.observacao}</div>` : ''}
        </div>
      `).join('')}
      <div class="divider"></div>
      <div>
        <strong>Total do pedido: R$ ${totalPedidoAtual.toFixed(2).replace('.', ',')}</strong>
      </div>
    </body>
    </html>
  `;
  
  // Abre uma nova janela para impressão
  const janelaImpressao = window.open('', '_blank');
  janelaImpressao.document.write(conteudoImpressao);
  janelaImpressao.document.close();
  
  // Imprime e fecha a janela após a impressão
  janelaImpressao.onload = function() {
    janelaImpressao.print();
    janelaImpressao.onafterprint = function() {
      janelaImpressao.close();
    };
  };
};

// Modifique a função handleEnviarPedido para incluir a impressão
const handleEnviarPedido = async () => {
  if (pedido.length === 0) {
    toast.warn("Adicione pelo menos um item ao pedido.");
    return;
  }
  const dadosDoPedido = {
    mesaId: mesa.id,
    itens: pedido.map(item => ({ produtoId: item.id, quantidade: item.quantidade, observacao: item.observacao })),
  };
  try {
    await apiClient.post('/api/pedidos', dadosDoPedido);
    toast.success("Pedido enviado com sucesso!");
    
    // Imprime o pedido antes de limpar os dados
    handlePrint();
    
    setPedido([]);
    fetchMesaEProdutos(); // Recarrega os dados da mesa para mostrar o novo pedido
  } catch (error) {
    toast.error("Erro ao enviar o pedido. Tente novamente.");
  }
};

  // Funções de manipulação do pedido (sem alterações na lógica)
  const handleAdicionarProduto = (produto, quantidade) => {
    setPedido(pedidoAtual => {
      const produtoExistente = pedidoAtual.find(item => item.id === produto.id);
      if (produtoExistente) {
        return pedidoAtual.map(item => item.id === produto.id ? { ...item, quantidade: item.quantidade + quantidade } : item);
      } else {
        return [...pedidoAtual, { ...produto, quantidade, observacao: '' }];
      }
    });
  };
  const handleRemoverItem = (itemId) => setPedido(p => p.filter(item => item.id !== itemId));
  const handleAtualizarObservacao = (itemId, obs) => setPedido(p => p.map(item => item.id === itemId ? { ...item, observacao: obs } : item));

  const totalPedidoAtual = pedido.reduce((acc, item) => acc + (item.preco * item.quantidade), 0);
  const totalPedidosAnteriores = mesa ? mesa.valorTotal : 0;
  const totalDaMesa = totalPedidosAnteriores + totalPedidoAtual;

  const cardapioFiltrado = useMemo(() => {
    return cardapio
      .filter(produto => filtroCategoria === 'Todos' || produto.categoria === filtroCategoria)
      .filter(produto => produto.nome.toLowerCase().includes(termoBusca.toLowerCase()));
  }, [cardapio, filtroCategoria, termoBusca]);

  if (!mesa) {
    return <div className="p-8 text-center"><p className="font-semibold text-gray-600">Carregando...</p></div>;
  }
  
  return (
    <div className="w-full p-4 md:p-6 max-w-screen-xl mx-auto">
      <DetalheMesaHeader mesa={mesa} onMesaUpdate={fetchMesaEProdutos} />
      
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-4">
          <input type="text" placeholder="🔎 Buscar no cardápio..." className="w-full p-3 border rounded-lg shadow-sm" value={termoBusca} onChange={e => setTermoBusca(e.target.value)} />
          <div className="flex gap-2 mt-4 overflow-x-auto pb-2">
            {categorias.map(cat => (<button key={cat} onClick={() => setFiltroCategoria(cat)} className={`px-4 py-2 rounded-full font-semibold text-sm whitespace-nowrap transition-colors ${filtroCategoria === cat ? 'bg-orange-500 text-white shadow' : 'bg-white text-gray-700 hover:bg-gray-100'}`}>{cat}</button>))}
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {cardapioFiltrado.map(produto => (<ProdutoCard key={produto.id} produto={produto} onAdicionar={handleAdicionarProduto} />))}
          </div>
        </div>
        <div className="bg-white p-4 rounded-lg shadow-md h-fit space-y-4">
          <h2 className="text-xl font-bold text-gray-800">Resumo do Pedido</h2>
          
          {mesa.pedidos && mesa.pedidos.length > 0 && (
            <div className="border-b pb-2">
              <h3 className="text-md font-semibold text-gray-600 mb-2">Pedidos Anteriores</h3>
              <div className="space-y-2 divide-y divide-gray-100">
                {mesa.pedidos.map((pedidoAnterior, index) => (
                  <React.Fragment key={pedidoAnterior.id}>
                    <div className="text-sm pt-2 first:pt-0">
                      <div className="flex justify-between font-bold text-gray-700"><span>Pedido #{index + 1}</span></div>
                      {pedidoAnterior.itens.map(item => (
                        <React.Fragment key={item.id}>
                          <div className="flex justify-between items-center pl-2">
                            <span className="text-gray-600">{item.quantidade}x {item.produto.nome}</span>
                            <span className="text-gray-800">R$ {(item.precoUnitario * item.quantidade).toFixed(2).replace('.', ',')}</span>
                          </div>
                          {item.observacao && (
                            <p className="text-xs text-orange-700 pl-4 italic">↳ Obs: {item.observacao}</p>
                          )}
                        </React.Fragment>
                      ))}
                    </div>
                  </React.Fragment>
                ))}
              </div>
            </div>
          )}

          <div>
            <h3 className="text-md font-semibold text-gray-600 mt-4">Novo Pedido</h3>
            {pedido.length === 0 ? (<p className="text-gray-500 text-sm mt-2">Nenhum item adicionado.</p>) : (
              <div className="mt-2 space-y-2 divide-y divide-gray-100">
                {pedido.map(item => (
                  <div key={item.id} className="text-sm pt-4 first:pt-0">
                    <div className="flex justify-between items-center">
                      <div>
                        <p className="font-bold text-gray-800">{item.quantidade}x {item.nome}</p>
                        <p className="text-gray-500">R$ {item.preco.toFixed(2).replace('.', ',')}</p>
                      </div>
                      <div className="flex items-center gap-2">
                        <p className="font-semibold text-gray-800">R$ {(item.preco * item.quantidade).toFixed(2).replace('.', ',')}</p>
                        <button onClick={() => handleRemoverItem(item.id)} className="text-red-500 hover:text-red-700">
                          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
                        </button>
                      </div>
                    </div>
                    <input type="text" placeholder="Adicionar observação..." value={item.observacao || ''} onChange={(e) => handleAtualizarObservacao(item.id, e.target.value)} className="mt-2 w-full text-xs p-1 border rounded bg-gray-50 focus:ring-1 focus:ring-orange-500 focus:border-orange-500" />
                  </div>
                ))}
              </div>
            )}
          </div>
          <div className="border-t-2 border-dashed pt-4 space-y-2 text-sm">
            <div className="flex justify-between text-gray-600"><span>Pedidos anteriores:</span><span>R$ {totalPedidosAnteriores.toFixed(2).replace('.', ',')}</span></div>
            <div className="flex justify-between text-gray-600"><span>Pedido atual:</span><span>R$ {totalPedidoAtual.toFixed(2).replace('.', ',')}</span></div>
            <div className="flex justify-between font-bold text-lg text-gray-900 mt-2"><span>Total da Mesa:</span><span>R$ {totalDaMesa.toFixed(2).replace('.', ',')}</span></div>
          </div>
          <button onClick={handleEnviarPedido} disabled={pedido.length === 0} className="w-full bg-orange-500 text-white font-bold py-3 rounded-lg hover:bg-orange-600 transition-colors disabled:bg-gray-400 disabled:cursor-not-allowed">Enviar Pedido</button>
        </div>
      </div>
    </div>
  );
};

export default DetalheMesaPage;